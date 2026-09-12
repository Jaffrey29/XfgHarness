package cn.xiaofuge.deepseek.harness.domain.agent.model.entity;

import cn.xiaofuge.deepseek.harness.domain.agent.model.valobj.ApprovalModeVO;

/**
 * Agent 运行时的模型渠道配置。
 */
public record AgentOptions(
        String channelCode,
        String provider,
        String model,
        Integer maxTokens,
        String baseUrl,
        String apiKey,
        String protocol,
        ApprovalModeVO approvalMode
) {
    public AgentOptions(String channelCode, String provider, String model, Integer maxTokens,
                        String baseUrl, String apiKey, String protocol) {
        this(channelCode, provider, model, maxTokens, baseUrl, apiKey, protocol,
                ApprovalModeVO.REQUEST_APPROVAL);
    }

    public AgentOptions {
        if (maxTokens != null && maxTokens <= 0) {
            throw new IllegalArgumentException("maxTokens must be a positive integer");
        }
    }
}
