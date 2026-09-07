package cn.xiaofuge.deepseek.harness.api.dto;

import java.time.Instant;

public record ConversationMessageResponseDTO(
        String role,
        String content,
        Instant occurredAt
) {
}
