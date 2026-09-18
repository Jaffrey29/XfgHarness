package cn.xiaofuge.deepseek.harness.infrastructure.digitalhuman;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 远端 Agent 网关：把任务转发给远端（或本机回环）DSH 的 /api/agent/stream，
 * 解析其 SSE 事件流并按统一语义回调（chunk/reasoning/tool/finish/error）。
 *
 * <p>只负责协议转换与事件抽取，协作语义（任务状态、事件落库）由
 * CollaborationService 承载。</p>
 */
public class RemoteAgentGateway {

    private static final Logger log = LoggerFactory.getLogger(RemoteAgentGateway.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    /** taskKey（remoteSessionId）-> 取消句柄 */
    private final Set<String> cancelled = ConcurrentHashMap.newKeySet();

    public RemoteAgentGateway() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /** 执行流事件回调 */
    public interface EventSink {
        void onChunk(String text);

        void onReasoning(String text);

        void onToolCall(String toolName, String summary);

        void onToolResult(String toolName, String summary);

        default void onToolResult(String toolName, String summary, String rawResult) {
            onToolResult(toolName, summary);
        }

        /** 执行在审批点中断：携带审批关键信息（若流内有），由协作层落房间事件 */
        default void onApprovalRequired(String toolName, String summary) {
        }
    }

    /** 执行在审批点中断（Agent 返回的流正常结束但任务实际等待审批） */
    public static class ApprovalInterruptException extends Exception {
        public ApprovalInterruptException(String message) {
            super(message);
        }
    }

    /** 远端数字人：POST {baseUrl}/api/agent/stream */
    public void runRemote(String baseUrl, String token, String instruction,
                          String channelCode, String approvalMode, EventSink sink) throws Exception {
        String url = baseUrl.replaceAll("/+$", "") + "/api/agent/stream";
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(buildBody(instruction, null, approvalMode)));
        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Bearer " + token.trim());
        }
        stream(builder.build(), sink);
    }

    /** A2A 数字人：优先 POST {baseUrl}/message/stream，失败回退 POST {baseUrl}/message/send。 */
    public void runA2a(String baseUrl, String token, String instruction, EventSink sink) throws Exception {
        String normalized = baseUrl.replaceAll("/+$", "");
        Exception lastError = null;
        try {
            streamA2a(a2aMessageRequest(normalized + "/message/stream", token, instruction)
                    .timeout(Duration.ofMinutes(10))
                    .build(), sink);
            return;
        } catch (Exception error) {
            lastError = error;
            log.debug("A2A message/stream failed, fallback: {}", error.getMessage());
        }

        try {
            sendA2a(a2aMessageRequest(normalized + "/message/send", token, instruction)
                    .timeout(Duration.ofMinutes(10))
                    .build(), sink);
            return;
        } catch (Exception error) {
            lastError = error;
            log.debug("A2A message/send failed, fallback: {}", error.getMessage());
        }

        try {
            streamA2a(a2aJsonRpcRequest(normalized, token, "tasks/sendSubscribe", instruction)
                    .timeout(Duration.ofMinutes(10))
                    .build(), sink);
            return;
        } catch (Exception error) {
            lastError = error;
            log.debug("A2A tasks/sendSubscribe failed, fallback: {}", error.getMessage());
        }

        try {
            sendA2a(a2aJsonRpcRequest(normalized, token, "tasks/send", instruction)
                    .timeout(Duration.ofMinutes(10))
                    .build(), sink);
            return;
        } catch (Exception error) {
            lastError = error;
        }
        throw lastError == null ? new IllegalStateException("A2A Agent 调用失败") : lastError;
    }

    private void sendA2a(HttpRequest request, EventSink sink) throws Exception {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("A2A Agent 返回 HTTP " + response.statusCode() + "：" + abbreviate(response.body()));
        }
        JsonNode payload = objectMapper.readTree(response.body());
        String text = extractA2aText(payload);
        if (!text.isBlank()) sink.onChunk(text);
    }

    private HttpRequest.Builder a2aMessageRequest(String url, String token, String instruction) throws Exception {
        Map<String, Object> body = Map.of(
                "message", Map.of(
                        "role", "user",
                        "parts", java.util.List.of(Map.of("kind", "text", "text", instruction))
                )
        );
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Bearer " + token.trim());
        }
        return builder;
    }

    private HttpRequest.Builder a2aJsonRpcRequest(String url, String token, String method, String instruction) throws Exception {
        Map<String, Object> body = Map.of(
                "jsonrpc", "2.0",
                "id", "a2a-" + System.currentTimeMillis(),
                "method", method,
                "params", Map.of(
                        "message", Map.of(
                                "role", "user",
                                "parts", java.util.List.of(Map.of("kind", "text", "text", instruction))
                        )
                )
        );
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Bearer " + token.trim());
        }
        return builder;
    }

    private void streamA2a(HttpRequest request, EventSink sink) throws Exception {
        HttpResponse<java.io.InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            String errorBody;
            try (var stream = response.body()) {
                errorBody = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
            throw new IllegalStateException("A2A Agent 返回 HTTP " + response.statusCode() + "：" + abbreviate(errorBody));
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            StringBuilder dataLines = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data:")) {
                    String value = line.substring(5);
                    if (value.startsWith(" ")) value = value.substring(1);
                    if (dataLines.length() > 0) dataLines.append("\n");
                    dataLines.append(value);
                } else if (line.isBlank()) {
                    if (dataLines.length() > 0) {
                        dispatchA2a(dataLines.toString(), sink);
                        dataLines.setLength(0);
                    }
                }
            }
            if (dataLines.length() > 0) {
                dispatchA2a(dataLines.toString(), sink);
            }
        }
    }

    private void dispatchA2a(String raw, EventSink sink) {
        try {
            JsonNode payload = objectMapper.readTree(raw);
            String text = extractA2aText(payload);
            if (!text.isBlank()) sink.onChunk(text);
        } catch (Exception e) {
            if (!raw.isBlank()) sink.onChunk(raw);
        }
    }

    /**
     * 本地数字人：回环调用本服务的 /api/agent/stream。
     * 通过运行时实际端口自连，避免在协作层重复实现 Agent 主循环。
     */
    public void runLocal(String instruction, String channelCode, String approvalMode, EventSink sink) throws Exception {
        String port = System.getProperty("server.port", "8080");
        String url = "http://127.0.0.1:" + port + "/api/agent/stream";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(buildBody(instruction, channelCode, approvalMode)))
                .build();
        stream(request, sink);
    }

    public void cancel(String taskKey) {
        if (taskKey != null) cancelled.add(taskKey);
    }

    private String buildBody(String instruction, String channelCode, String approvalMode) throws Exception {
        var body = new java.util.LinkedHashMap<String, Object>();
        body.put("agentId", "collab-" + System.currentTimeMillis());
        body.put("message", instruction);
        if (channelCode != null && !channelCode.isBlank()) {
            body.put("channelCode", channelCode);
        }
        body.put("approvalMode", approvalMode == null || approvalMode.isBlank() ? "AUTO_APPROVE" : approvalMode);
        return objectMapper.writeValueAsString(body);
    }

    /** 消费 DSH SSE 流：event: xxx + data: {json}，空行分隔 */
    private void stream(HttpRequest request, EventSink sink) throws Exception {
        HttpResponse<java.io.InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            String errorBody;
            try (var stream = response.body()) {
                errorBody = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
            throw new IllegalStateException("Agent 服务返回 HTTP " + response.statusCode() + "：" + abbreviate(errorBody));
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String eventName = "";
            StringBuilder dataLines = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("event:")) {
                    eventName = line.substring(6).trim();
                } else if (line.startsWith("data:")) {
                    String value = line.substring(5);
                    if (value.startsWith(" ")) value = value.substring(1);
                    if (dataLines.length() > 0) dataLines.append("\n");
                    dataLines.append(value);
                } else if (line.isBlank()) {
                    if (dataLines.length() > 0) {
                        dispatch(eventName, dataLines.toString(), sink);
                        dataLines.setLength(0);
                        eventName = "";
                    }
                }
            }
            if (dataLines.length() > 0) {
                dispatch(eventName, dataLines.toString(), sink);
            }
        }
    }

    private void dispatch(String eventName, String raw, EventSink sink) {
        try {
            JsonNode payload = objectMapper.readTree(raw);
            switch (eventName == null || eventName.isBlank() ? "chunk" : eventName) {
                case "chunk" -> sink.onChunk(extractText(payload));
                case "reasoning" -> sink.onReasoning(extractText(payload));
                case "tool_result" -> {
                    String toolName = payload.path("toolName").asText(
                            payload.path("name").asText("tool"));
                    JsonNode resultNode = payload.path("result");
                    String rawResult = resultNode.isMissingNode() ? "" : rawJson(resultNode);
                    String summary = abbreviate(resultNode.isMissingNode() ? extractText(payload) : extractText(resultNode));
                    // tool_result 事件在 DSH 中既承载发起也承载返回，按 status 区分
                    String status = payload.path("status").asText("");
                    if ("running".equals(status) || "call".equals(status)) {
                        sink.onToolCall(toolName, summary);
                    } else {
                        sink.onToolResult(toolName, summary, rawResult);
                    }
                }
                case "step_break" -> {
                    String toolName = payload.path("toolName").asText("tool");
                    String status = payload.path("status").asText("");
                    String summary = abbreviate(payload.toString());
                    // 审批中断信号：工具调用进入审批等待（发起后无对应 result 即被挂起）
                    if (status.contains("approval") || status.contains("pending")
                            || "waiting".equals(status) || "requires_approval".equals(status)) {
                        sink.onApprovalRequired(toolName, summary);
                    } else {
                        sink.onToolCall(toolName, summary);
                    }
                }
                case "finish", "done" -> {
                    // 完成信号由调用方收尾，无需转发
                }
                case "error" -> throw new IllegalStateException(
                        payload.path("message").asText(extractText(payload)));
                default -> {
                    // meta 等事件忽略
                }
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            // 非 JSON 数据按纯文本 chunk 处理
            if ("chunk".equals(eventName) || eventName == null || eventName.isBlank()) {
                sink.onChunk(raw);
            } else {
                log.debug("Skip unparsable SSE event {}: {}", eventName, e.getMessage());
            }
        }
    }

    private String extractText(JsonNode payload) {
        if (payload.isTextual()) return payload.asText();
        for (String field : new String[]{"text", "content", "delta", "message", "result"}) {
            JsonNode node = payload.path(field);
            if (node.isTextual()) return node.asText();
        }
        return payload.toString();
    }

    private String extractA2aText(JsonNode payload) {
        if (payload == null || payload.isMissingNode() || payload.isNull()) return "";
        if (payload.isTextual()) return payload.asText();
        JsonNode result = payload.path("result");
        if (!result.isMissingNode()) {
            String text = extractA2aText(result);
            if (!text.isBlank()) return text;
        }
        JsonNode message = payload.path("message");
        if (!message.isMissingNode()) {
            String text = extractA2aText(message);
            if (!text.isBlank()) return text;
        }
        JsonNode artifact = payload.path("artifact");
        if (!artifact.isMissingNode()) {
            String text = extractA2aText(artifact);
            if (!text.isBlank()) return text;
        }
        JsonNode parts = payload.path("parts");
        if (parts.isArray()) {
            StringBuilder builder = new StringBuilder();
            parts.forEach(part -> {
                String text = part.path("text").asText("");
                if (!text.isBlank()) builder.append(text);
            });
            if (builder.length() > 0) return builder.toString();
        }
        for (String field : new String[]{"text", "content", "delta", "message", "result"}) {
            JsonNode node = payload.path(field);
            if (node.isTextual()) return node.asText();
        }
        return "";
    }

    private String rawJson(JsonNode payload) {
        if (payload == null || payload.isMissingNode() || payload.isNull()) return "";
        return payload.isTextual() ? payload.asText() : payload.toString();
    }

    private String abbreviate(String text) {
        if (text == null) return "";
        String singleLine = text.replaceAll("\\s+", " ").trim();
        return singleLine.length() > 200 ? singleLine.substring(0, 200) + "…" : singleLine;
    }
}
