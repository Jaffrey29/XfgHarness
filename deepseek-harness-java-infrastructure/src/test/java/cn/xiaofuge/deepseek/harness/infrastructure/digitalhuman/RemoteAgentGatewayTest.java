package cn.xiaofuge.deepseek.harness.infrastructure.digitalhuman;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RemoteAgentGatewayTest {

    private HttpServer server;

    @Test
    void runRemoteDoesNotForwardLocalChannelCode() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/agent/stream", exchange -> {
            String request = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(request.contains("远端执行"));
            assertFalse(request.contains("channelCode"));
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream body = exchange.getResponseBody()) {
                body.write("event: chunk\ndata: {\"content\":\"远端完成\"}\n\n".getBytes(StandardCharsets.UTF_8));
                body.write("event: done\ndata: {}\n\n".getBytes(StandardCharsets.UTF_8));
                body.flush();
            }
        });
        server.start();

        List<String> chunks = new ArrayList<>();
        new RemoteAgentGateway().runRemote(baseUrl(), "", "远端执行", "local-channel", "AUTO_APPROVE", sink(chunks));

        assertEquals(List.of("远端完成"), chunks);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void runsA2aAgentOverMessageStream() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/message/stream", exchange -> {
            assertTrue(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8).contains("巡检服务器"));
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream body = exchange.getResponseBody()) {
                body.write("data: {\"result\":{\"message\":{\"parts\":[{\"kind\":\"text\",\"text\":\"收到，\"}]}}}\n\n".getBytes(StandardCharsets.UTF_8));
                body.write("data: {\"result\":{\"message\":{\"parts\":[{\"kind\":\"text\",\"text\":\"巡检完成\"}]}}}\n\n".getBytes(StandardCharsets.UTF_8));
                body.flush();
            }
        });
        server.start();

        List<String> chunks = new ArrayList<>();
        new RemoteAgentGateway().runA2a(baseUrl(), "token-1", "巡检服务器", sink(chunks));

        assertEquals("收到，巡检完成", String.join("", chunks));
    }

    @Test
    void fallsBackToA2aMessageSendWhenStreamUnavailable() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/message/stream", exchange -> respondText(exchange, 404, "not found"));
        server.createContext("/message/send", exchange -> {
            String authorization = exchange.getRequestHeaders().getFirst("Authorization");
            assertEquals("Bearer token-2", authorization);
            assertTrue(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8).contains("生成报告"));
            respondJson(exchange, 200, "{\"result\":{\"parts\":[{\"kind\":\"text\",\"text\":\"报告已生成\"}]}} ");
        });
        server.start();

        List<String> chunks = new ArrayList<>();
        new RemoteAgentGateway().runA2a(baseUrl(), "token-2", "生成报告", sink(chunks));

        assertEquals(List.of("报告已生成"), chunks);
    }

    @Test
    void fallsBackToJsonRpcSendSubscribeWhenMessageEndpointsUnavailable() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/message/stream", exchange -> respondText(exchange, 404, "not found"));
        server.createContext("/message/send", exchange -> respondText(exchange, 404, "not found"));
        server.createContext("/", exchange -> {
            String request = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(request.contains("tasks/sendSubscribe"));
            assertTrue(request.contains("检查数据库"));
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream body = exchange.getResponseBody()) {
                body.write("data: {\"result\":{\"parts\":[{\"kind\":\"text\",\"text\":\"数据库正常\"}]}}\n\n".getBytes(StandardCharsets.UTF_8));
                body.flush();
            }
        });
        server.start();

        List<String> chunks = new ArrayList<>();
        new RemoteAgentGateway().runA2a(baseUrl(), "", "检查数据库", sink(chunks));

        assertEquals(List.of("数据库正常"), chunks);
    }

    @Test
    void fallsBackToJsonRpcSendWhenSubscribeUnavailable() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/message/stream", exchange -> respondText(exchange, 404, "not found"));
        server.createContext("/message/send", exchange -> respondText(exchange, 404, "not found"));
        server.createContext("/", exchange -> {
            String request = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            if (request.contains("tasks/sendSubscribe")) {
                respondText(exchange, 404, "not found");
                return;
            }
            assertTrue(request.contains("tasks/send"));
            assertTrue(request.contains("整理纪要"));
            respondJson(exchange, 200, "{\"result\":{\"message\":{\"parts\":[{\"kind\":\"text\",\"text\":\"纪要已整理\"}]}}}");
        });
        server.start();

        List<String> chunks = new ArrayList<>();
        new RemoteAgentGateway().runA2a(baseUrl(), "", "整理纪要", sink(chunks));

        assertEquals(List.of("纪要已整理"), chunks);
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private RemoteAgentGateway.EventSink sink(List<String> chunks) {
        return new RemoteAgentGateway.EventSink() {
            @Override public void onChunk(String text) { chunks.add(text); }
            @Override public void onReasoning(String text) { }
            @Override public void onToolCall(String toolName, String summary) { }
            @Override public void onToolResult(String toolName, String summary) { }
        };
    }

    private void respondJson(HttpExchange exchange, int status, String body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        respondText(exchange, status, body);
    }

    private void respondText(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
