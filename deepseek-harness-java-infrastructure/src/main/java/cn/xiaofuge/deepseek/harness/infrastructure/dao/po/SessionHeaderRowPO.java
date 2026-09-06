package cn.xiaofuge.deepseek.harness.infrastructure.dao.po;

import java.time.Instant;

/**
 * 会话头的持久化记录。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record SessionHeaderRowPO(
        String sessionId,
        int version,
        Instant createdAt,
        String cwd,
        String parentSession,
        int seedLength,
        String origin,
        int delegationDepth,
        String agentPreset
) {}
