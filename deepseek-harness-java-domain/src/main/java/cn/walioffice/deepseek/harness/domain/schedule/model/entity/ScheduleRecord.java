package cn.walioffice.deepseek.harness.domain.schedule.model.entity;

import cn.walioffice.deepseek.harness.domain.schedule.model.valobj.ScheduleKind;
import java.time.Instant;

/**
 * Durable reminder record. One-shot kinds ({@link ScheduleKind#AFTER},
 * {@link ScheduleKind#AT}) terminate on dispatch; {@link ScheduleKind#EVERY}
 * is a fixed-rate recurring reminder.
 *
 * @param id             session-local stable identity
 * @param kind           rule discriminator
 * @param prompt         trimmed reminder content
 * @param afterSeconds   positive delay (AFTER only)
 * @param everySeconds   fixed interval >= 300 (EVERY only)
 * @param scheduledAt    next target instant (RFC 3339 UTC)
 * @param sessionId      owning session
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record ScheduleRecord(
        String id,
        ScheduleKind kind,
        String prompt,
        Long afterSeconds,
        Long everySeconds,
        Instant scheduledAt,
        String sessionId
) {
    public ScheduleRecord {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("prompt must not be blank");
        }
    }
}
