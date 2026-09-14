package cn.xiaofuge.deepseek.harness.api.gateway.dto;

import cn.xiaofuge.deepseek.harness.api.dto.AgentMessageRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.StartWorkflowRequestDTO;

public record GatewayStreamRequestDTO(
        String source,
        AgentMessageRequestDTO agent,
        StartWorkflowRequestDTO workflow
) {
    public GatewayStreamRequestDTO {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("source must be non-blank");
        }
    }
}
