package cn.xiaofuge.deepseek.harness.infrastructure.digitalhuman;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DigitalHumanServiceA2aDiscoveryTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void discoversA2aAgentCardAndMapsCapabilities() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/.well-known/agent.json", exchange -> respondJson(exchange, 200, """
                {
                  "name": "需求分析 Agent",
                  "description": "负责需求澄清与验收标准",
                  "protocolVersion": "0.3.0",
                  "skills": [
                    {"name": "需求拆解", "tags": ["prd", "acceptance"]},
                    {"name": "风险识别", "tags": ["risk"]}
                  ]
                }
                """));
        server.start();

        Map<String, Object> result = new DigitalHumanService(null, null)
                .discover(baseUrl(), "a2a", null, null);

        assertEquals(true, result.get("reachable"));
        assertEquals("0.3.0", result.get("protocolVersion"));
        Map<String, Object> suggested = (Map<String, Object>) result.get("suggested");
        assertEquals("需求分析 Agent", suggested.get("displayName"));
        assertEquals("负责需求澄清与验收标准", suggested.get("purpose"));
        List<String> tags = (List<String>) suggested.get("roleTags");
        assertTrue(tags.contains("需求拆解"));
        assertTrue(tags.contains("acceptance"));
        assertEquals("WRITE_REQUIRES_APPROVAL", suggested.get("approvalPolicy"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void reportsUnauthorizedWhenA2aAgentCardRejectsToken() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/.well-known/agent.json", exchange -> {
            assertEquals("Bearer bad-token", exchange.getRequestHeaders().getFirst("Authorization"));
            respondJson(exchange, 401, "{\"error\":\"unauthorized\"}");
        });
        server.start();

        Map<String, Object> result = new DigitalHumanService(null, null)
                .discover(baseUrl(), "a2a", null, "bad-token");

        assertEquals(false, result.get("reachable"));
        Map<String, Object> error = (Map<String, Object>) result.get("error");
        assertEquals("unauthorized", error.get("kind"));
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
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
