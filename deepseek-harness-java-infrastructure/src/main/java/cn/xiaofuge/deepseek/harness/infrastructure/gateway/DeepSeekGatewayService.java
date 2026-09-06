package cn.xiaofuge.deepseek.harness.infrastructure.gateway;

import cn.xiaofuge.deepseek.harness.domain.runtime.setting.IModelSettingQueryService;
import cn.xiaofuge.deepseek.harness.domain.task.execution.model.valobj.ModelExecutionResultVO;
import cn.xiaofuge.deepseek.harness.infrastructure.gateway.dto.ModelRequestDTO;
import cn.xiaofuge.deepseek.harness.infrastructure.gateway.dto.ModelResponseDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 深度求索网关服务。
 * <p>
 * 执行流程：
 * 1. 接收模型画像或对话请求；
 * 2. 组装请求并调用外部模型服务；
 * 3. 返回模型响应或提供方信息。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class DeepSeekGatewayService {

    private static final String DEFAULT_CHAT_COMPLETIONS_URL = "https://api.deepseek.com/v1/chat/completions";

    private final IModelSettingQueryService modelSettingService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String defaultModel;

    public DeepSeekGatewayService(IModelSettingQueryService modelSettingService,
                                  ObjectMapper objectMapper,
                                  @Value("${harness.llm.deepseek.default-model:codex}") String defaultModel) {
        this.modelSettingService = modelSettingService;
        this.objectMapper = objectMapper;
        this.defaultModel = defaultModel;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    /**
     * 解析指定画像对应的提供方和模型。
     * <p>
     * 执行流程：
     * 1. 读取模型请求；
     * 2. 调用外部服务解析提供方与模型；
     * 3. 返回解析结果。
     *
     * @param request 模型请求
     * @return 解析后的提供方和模型信息
     */
    public ModelResponseDTO resolveModel(ModelRequestDTO request) {
        var configuredSetting = modelSettingService.findByProfileCode(request.profileCode())
                .filter(setting -> setting.enabled());
        if (configuredSetting.isPresent()) {
            return new ModelResponseDTO(configuredSetting.get().providerCode(), configuredSetting.get().modelCode());
        }
        return new ModelResponseDTO("deepseek", defaultModel);
    }

    /**
     * 执行一次对话补全请求。
     * <p>
     * 执行流程：
     * 1. 读取执行画像和提示词；
     * 2. 调用配置好的提供方执行请求；
     * 3. 返回助手回复和提供方元数据。
     *
     * @param profileCode 执行画像编码
     * @param prompt 用户提示词
     * @return 助手回复和提供方元数据
     */
    public ModelExecutionResultVO execute(String profileCode, String prompt) {
        var setting = modelSettingService.findByProfileCode(profileCode)
                .filter(modelSetting -> modelSetting.enabled())
                .orElseThrow(() -> new IllegalStateException("model setting is missing or disabled for profile: " + profileCode));

        try {
            String requestBody = objectMapper.writeValueAsString(java.util.Map.of(
                    "model", setting.modelCode(),
                    "messages", java.util.List.of(java.util.Map.of("role", "user", "content", prompt))
            ));
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(resolveChatCompletionsUri(setting.baseUrl()))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody));
            if (setting.apiKeyRef() != null && !setting.apiKeyRef().isBlank()) {
                builder.header("Authorization", "Bearer " + setting.apiKeyRef());
            }

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("provider returned HTTP " + response.statusCode() + ": " + extractErrorMessage(response.body()));
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = extractAssistantContent(root);
            String responseModel = root.path("model").asText(setting.modelCode());
            return new ModelExecutionResultVO(setting.providerCode(), responseModel, content);
        } catch (IOException exception) {
            throw new IllegalStateException("failed to encode or decode provider payload", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("provider request interrupted", exception);
        }
    }

    private URI resolveChatCompletionsUri(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return URI.create(DEFAULT_CHAT_COMPLETIONS_URL);
        }
        String normalized = baseUrl.trim();
        if (normalized.endsWith("/chat/completions")) {
            return URI.create(normalized);
        }
        if (normalized.endsWith("/v1")) {
            return URI.create(normalized + "/chat/completions");
        }
        if (normalized.endsWith("/")) {
            return URI.create(normalized + "v1/chat/completions");
        }
        return URI.create(normalized + "/v1/chat/completions");
    }

    private String extractAssistantContent(JsonNode root) {
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        String content = flattenContent(contentNode);
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("provider response did not contain assistant content");
        }
        return content;
    }

    private String flattenContent(JsonNode contentNode) {
        if (contentNode == null || contentNode.isMissingNode() || contentNode.isNull()) {
            return null;
        }
        if (contentNode.isTextual()) {
            return contentNode.asText();
        }
        if (contentNode.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode item : contentNode) {
                String piece = item.path("text").asText(null);
                if (piece == null || piece.isBlank()) {
                    piece = item.path("content").asText(null);
                }
                if (piece != null && !piece.isBlank()) {
                    if (!builder.isEmpty()) {
                        builder.append('\n');
                    }
                    builder.append(piece);
                }
            }
            return builder.isEmpty() ? null : builder.toString();
        }
        return contentNode.toString();
    }

    private String extractErrorMessage(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String message = root.path("error").path("message").asText(null);
            if (message != null && !message.isBlank()) {
                return message;
            }
        } catch (IOException ignored) {
            return responseBody;
        }
        return responseBody;
    }
}
