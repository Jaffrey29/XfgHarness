package cn.xiaofuge.deepseek.harness.api.dto;

public record AgentToolCallEventResponseDTO(
        String callId,
        String toolName,
        String arguments,
        String status
) {}
