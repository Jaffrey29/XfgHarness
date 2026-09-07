package cn.xiaofuge.deepseek.harness.domain.agent.model.entity;

/**
 * AgentOptions 表示智能体运行时的领域配置实体。
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
