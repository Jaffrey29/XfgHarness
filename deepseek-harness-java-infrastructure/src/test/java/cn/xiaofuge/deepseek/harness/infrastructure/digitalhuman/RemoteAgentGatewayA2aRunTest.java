package cn.xiaofuge.deepseek.harness.infrastructure.digitalhuman;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归：A2A 调用必须先经 Agent Card 发现 JSON-RPC 端点（{base}/a2a），
 * 而不是向根路径 / 或 /message/* 乱发请求（后者会得到 404/405，见 2026-09-19 故障）。
 */
class RemoteAgentGatewayA2aRunTest {

    private HttpServer server;
    private final AtomicInteger rootPostHits = new AtomicInteger();

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void runA2aDiscoversJsonRpcEndpointAndStreamsReply() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        String base = "http://127.0.0.1:" + server.getAddress().getPort();

        // 标准 Agent Card：url 指向 JSON-RPC 端点 /a2a
        server.createContext("/.well-known/agent-card.json", exchange ->
                respondJson(exchange, 200, "{\"name\":\"test\",\"url\":\"" + base + "/a2a\"}"));

        // JSON-RPC 端点：message/stream 返回 SSE（Task → artifact-update → final status-update）
        server.createContext("/a2a", exchange -> {
            if (!"POST".equals(exchange.getRequestMethod())) {
                respondJson(exchange, 405, "{\"error\":\"method not allowed\"}");
                return;
            }
            byte[] body = exchange.getRequestBody().readAllBytes();
            assertTrue(new String(body, StandardCharsets.UTF_8).contains("message/stream"));
            StringBuilder sse = new StringBuilder();
            sse.append("data: {\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"kind\":\"task\",\"id\":\"t1\",")
                    .append("\"contextId\":\"c1\",\"status\":{\"state\":\"working\"}}}\n\n");
            sse.append("data: {\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"kind\":\"artifact-update\",")
                    .append("\"artifact\":{\"artifactId\":\"a1\",\"parts\":[{\"kind\":\"text\",\"text\":\"你好，A2A\"}]}}}\n\n");
            sse.append("data: {\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"kind\":\"status-update\",")
                    .append("\"status\":{\"state\":\"completed\"},\"final\":true}}\n\n");
            byte[] bytes = sse.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });

        // 故障现场模拟：根路径只接受 GET，POST 应得到 405 —— 修复后客户端不应再打到这里
        server.createContext("/", exchange -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                rootPostHits.incrementAndGet();
                respondJson(exchange, 405, "{\"status\":405,\"error\":\"Method Not Allowed\",\"path\":\"/\"}");
                return;
            }
            respondJson(exchange, 200, "{}");
        });
        server.start();

        StringBuilder chunks = new StringBuilder();
        new RemoteAgentGateway().runA2a(base, null, "分析下问题", new RemoteAgentGateway.EventSink() {
            @Override public void onChunk(String text) { chunks.append(text); }
            @Override public void onReasoning(String text) { }
            @Override public void onToolCall(String toolName, String summary) { }
            @Override public void onToolResult(String toolName, String summary) { }
        });

        assertEquals("你好，A2A", chunks.toString());
        assertEquals(0, rootPostHits.get());
    }

    private void respondJson(HttpExchange exchange, int status, String body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
