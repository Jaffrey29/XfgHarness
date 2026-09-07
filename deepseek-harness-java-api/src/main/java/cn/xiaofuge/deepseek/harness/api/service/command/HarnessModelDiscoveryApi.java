package cn.xiaofuge.deepseek.harness.api.service.command;

import cn.xiaofuge.deepseek.harness.api.command.settings.DiscoverModelsRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 代理上游模型发现，前端可以同步可用模型并避免浏览器 CORS 限制。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class HarnessModelDiscoveryApi {

    private static final Logger log = LoggerFactory.getLogger(HarnessModelDiscoveryApi.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    /**
     * 处理Harness Model Discovery Api相关逻辑。
     */
    public HarnessModelDiscoveryApi(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    /**
     * 从上游 OpenAI-compatible 接口发现可用模型。
     */
    public List<String> discover(DiscoverModelsRequest request) {
        String baseUrl = normalizeBaseUrl(request.baseUrl());
        String apiKey = request.apiKeyRef();

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(resolveModelsUri(baseUrl))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .GET();
        if (apiKey != null && !apiKey.isBlank()) {
            builder.header("Authorization", "Bearer " + apiKey);
        }

        try {
            HttpResponse<String> response = httpClient.send(
                    builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Upstream returned HTTP " + response.statusCode() + ": " + extractError(response.body()));
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode data = root.path("data");
            if (!data.isArray()) {
                return List.of();
            }
            List<String> models = new ArrayList<>();
            for (JsonNode node : data) {
                String id = node.path("id").asText(null);
                if (id != null && !id.isBlank()) {
                    models.add(id);
                }
            }
            return models.stream().sorted().toList();
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Failed to discover models from {}: {}", baseUrl, e.getMessage());
            throw new IllegalStateException("无法连接到上游服务: " + e.getMessage(), e);
        }
    }

    /**
     * 处理normalize Base Url相关逻辑。
     */
    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("Base URL 不能为空");
        }
        return baseUrl.trim();
    }

    /**
     * 解析e Models Uri。
     */
    private URI resolveModelsUri(String baseUrl) {
        String normalized = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
        if (normalized.endsWith("/models")) {
            return URI.create(normalized);
        }
        if (normalized.endsWith("/v1")) {
            return URI.create(normalized + "/models");
        }
        return URI.create(normalized + "/v1/models");
    }

    /**
     * 处理extract Error相关逻辑。
     */
    private String extractError(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            String msg = root.path("error").path("message").asText(null);
            if (msg != null && !msg.isBlank()) {
                return msg;
            }
        } catch (Exception ignored) {
        }
        return body != null && body.length() > 200 ? body.substring(0, 200) + "…" : body;
    }
}
