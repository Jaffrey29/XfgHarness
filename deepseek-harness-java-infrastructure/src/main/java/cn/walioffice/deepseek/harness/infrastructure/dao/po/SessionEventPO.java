package cn.walioffice.deepseek.harness.infrastructure.dao.po;

import java.time.Instant;

/**
 * 保存一条追加写的会话事件记录。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record SessionEventPO(
        String sessionId,
        String eventType,
        String payload,
        Instant occurredAt
) {
}
