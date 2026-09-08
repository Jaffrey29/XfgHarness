package cn.xiaofuge.deepseek.harness.domain.agent.model.entity;

import cn.xiaofuge.deepseek.harness.domain.agent.model.valobj.ApprovalModeVO;

/**
 * AgentOptions 表示智能体运行时的领域配置实体。
 */
public record AgentOptions(
        String provider,
        String model,
        Integer maxTokens,
        String baseUrl,
        String apiKey,
        ApprovalModeVO approvalMode
) {
    public AgentOptions(String provider, String model, Integer maxTokens, String baseUrl, String apiKey) {
        this(provider, model, maxTokens, baseUrl, apiKey, ApprovalModeVO.REQUEST_APPROVAL);
    }
    public AgentOptions {
        if (maxTokens != null && (maxTokens <= 0)) {
            throw new IllegalArgumentException("maxTokens must be a positive integer");
        }
    }
}
