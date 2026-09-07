package cn.xiaofuge.deepseek.harness.api.dto;

import java.time.Instant;

public record ApprovalActionResponseDTO(
        String sessionId,
        String status,
        String message,
        Instant operatedAt
) {
}
