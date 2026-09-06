package cn.walioffice.deepseek.harness.domain.session.log.model.entity;

import java.time.Instant;

/**
 * Captures a durable session event entry.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record SessionEventEntity(
        String sessionId,
        String eventType,
        String payload,
        Instant occurredAt
) {
}
