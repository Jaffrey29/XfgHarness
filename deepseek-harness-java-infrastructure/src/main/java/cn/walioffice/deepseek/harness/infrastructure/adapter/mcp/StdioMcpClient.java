package cn.walioffice.deepseek.harness.infrastructure.adapter.mcp;

import cn.walioffice.deepseek.harness.domain.tool.mcp.IMcpClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Stdio-based MCP client implementation.
 * <p>
 * Communicates with an MCP server via newline-delimited JSON-RPC 2.0 over
 * the child process's stdin/stdout. Mirrors TS stdio transport.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class StdioMcpClient implements IMcpClient {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String command;
    private final List<String> args;
    private final Map<String, String> env;
    private final String cwd;
    private final long callTimeoutMs;

    private Process process;
    private BufferedWriter writer;
    private BufferedReader reader;
    private final AtomicLong nextId = new AtomicLong(1);
    private final Map<Long, CompletableFuture<JsonNode>> pending = new ConcurrentHashMap<>();
    private Thread readerThread;
    private volatile boolean connected = false;

    public StdioMcpClient(String command, List<String> args, Map<String, String> env, String cwd, long callTimeoutMs) {
        this.command = command;
        this.args = args != null ? args : List.of();
        this.env = env != null ? env : Map.of();
        this.cwd = cwd;
        this.callTimeoutMs = callTimeoutMs;
    }

    @Override
    public CompletableFuture<Void> connect() {
        return CompletableFuture.runAsync(() -> {
            try {
                List<String> cmd = new ArrayList<>();
                cmd.add(command);
                cmd.addAll(args);

                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.environment().putAll(env);
                if (cwd != null && !cwd.isBlank()) {
                    pb.directory(new File(cwd));
                }
                pb.redirectErrorStream(false);

                process = pb.start();
                writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
                reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                // Start reader thread
                readerThread = new Thread(this::readLoop, "mcp-stdio-reader");
                readerThread.setDaemon(true);
                readerThread.start();

                // Send initialize
                JsonNode initResult = sendAndWait("initialize", Map.of(
                        "protocolVersion", "2024-11-05",
                        "capabilities", Map.of(),
                        "clientInfo", Map.of("name", "deepseek-harness-java", "version", "1.0.0")
                )).get(callTimeoutMs, TimeUnit.MILLISECONDS);

                // Send initialized notification
                sendNotification("notifications/initialized", Map.of());

                connected = true;
            } catch (Exception e) {
                connected = false;
                throw new RuntimeException("Failed to connect to MCP server: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> disconnect() {
        return CompletableFuture.runAsync(() -> {
            connected = false;
            try {
                if (process != null && process.isAlive()) {
                    process.destroy();
                    if (!process.waitFor(5, TimeUnit.SECONDS)) {
                        process.destroyForcibly();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                pending.values().forEach(f -> f.completeExceptionally(new RuntimeException("Disconnected")));
                pending.clear();
            }
        });
    }

    @Override
    public boolean isConnected() {
        return connected && process != null && process.isAlive();
    }

    @Override
    public CompletableFuture<List<McpToolDescriptor>> listTools() {
        return send("tools/list", Map.of())
                .thenApply(result -> {
                    List<McpToolDescriptor> tools = new ArrayList<>();
                    JsonNode toolsNode = result.path("tools");
                    if (toolsNode.isArray()) {
                        for (JsonNode toolNode : toolsNode) {
                            String name = toolNode.path("name").asText();
                            String description = toolNode.path("description").asText("");
                            Map<String, Object> inputSchema = objectMapper.convertValue(
                                    toolNode.path("inputSchema"), Map.class);
                            tools.add(new McpToolDescriptor(name, description, inputSchema));
                        }
                    }
                    return tools;
                });
    }

    @Override
    public CompletableFuture<McpToolResult> callTool(String toolName, Map<String, Object> arguments) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", toolName);
        params.put("arguments", arguments);

        return send("tools/call", params)
                .thenApply(result -> {
                    List<Map<String, Object>> content = new ArrayList<>();
                    JsonNode contentNode = result.path("content");
                    if (contentNode.isArray()) {
                        for (JsonNode item : contentNode) {
                            content.add(objectMapper.convertValue(item, Map.class));
                        }
                    }
                    boolean isError = result.path("isError").asBoolean(false);
                    return new McpToolResult(content, isError);
                });
    }

    // ── JSON-RPC transport ───────────────────────────────────────

    private CompletableFuture<JsonNode> send(String method, Map<String, Object> params) {
        long id = nextId.getAndIncrement();
        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", id);
        request.put("method", method);
        request.set("params", objectMapper.valueToTree(params));

        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        pending.put(id, future);

        try {
            String json = objectMapper.writeValueAsString(request);
            synchronized (writer) {
                writer.write(json);
                writer.newLine();
                writer.flush();
            }
        } catch (IOException e) {
            pending.remove(id);
            future.completeExceptionally(new RuntimeException("Failed to send request: " + e.getMessage(), e));
        }

        // Timeout
        return future.orTimeout(callTimeoutMs, TimeUnit.MILLISECONDS);
    }

    private void sendNotification(String method, Map<String, Object> params) {
        try {
            ObjectNode notification = objectMapper.createObjectNode();
            notification.put("jsonrpc", "2.0");
            notification.put("method", method);
            notification.set("params", objectMapper.valueToTree(params));

            String json = objectMapper.writeValueAsString(notification);
            synchronized (writer) {
                writer.write(json);
                writer.newLine();
                writer.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to send notification: " + e.getMessage(), e);
        }
    }

    private CompletableFuture<JsonNode> sendAndWait(String method, Map<String, Object> params)
            throws Exception {
        return send(method, params);
    }

    private void readLoop() {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                try {
                    JsonNode message = objectMapper.readTree(line);

                    // Check if it's a response (has "id" and "result" or "error")
                    if (message.has("id")) {
                        long id = message.get("id").asLong();
                        CompletableFuture<JsonNode> future = pending.remove(id);
                        if (future != null) {
                            if (message.has("error")) {
                                JsonNode error = message.get("error");
                                future.completeExceptionally(new RuntimeException(
                                        "JSON-RPC error " + error.path("code").asInt() + ": "
                                                + error.path("message").asText()));
                            } else {
                                future.complete(message.path("result"));
                            }
                        }
                    }
                    // Notifications from server (no "id") are ignored for now
                } catch (Exception e) {
                    // Skip malformed lines
                }
            }
        } catch (IOException e) {
            // Stream closed — this is normal during disconnect
        } finally {
            connected = false;
            pending.values().forEach(f -> f.completeExceptionally(new RuntimeException("Connection closed")));
            pending.clear();
        }
    }
}
