package cn.walioffice.deepseek.harness.infrastructure.dao.po;

import java.time.Instant;

/**
 * 保存一条持久化的 Harness 会话记录。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record HarnessSessionPO(
        String sessionId,
        String workspaceId,
        String profileCode,
        String executionMode,
        String resolvedModel,
        String status,
        Instant createdAt
) {
}
