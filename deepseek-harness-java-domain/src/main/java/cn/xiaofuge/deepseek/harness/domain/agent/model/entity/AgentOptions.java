package cn.xiaofuge.deepseek.harness.domain.agent.model.entity;

/**
 * Agent creation options. Mirrors TS {@code AgentOptions}.
 *
 * @param provider  provider route (must have a registered adapter at call time)
 * @param model     model id interpreted by the selected provider adapter
 * @param maxTokens maximum output tokens for each conversation-model request
 * @param baseUrl   per-request upstream base URL (null = adapter default)
 * @param apiKey    per-request upstream API key (null = adapter default)
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record AgentOptions(
        String provider,
        String model,
        Integer maxTokens,
        String baseUrl,
        String apiKey
) {
    public AgentOptions {
        if (maxTokens != null && (maxTokens <= 0)) {
            throw new IllegalArgumentException("maxTokens must be a positive integer");
        }
    }
}
