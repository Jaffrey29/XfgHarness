package cn.xiaofuge.deepseek.harness.domain.task.approval.model.valobj;

import java.time.Instant;

/**
 * Represents one pending approval item for the web console.
 *
 * @param sessionId session identifier
 * @param title conversation title
 * @param preview last visible message preview
 * @param status current approval status
 * @param createdAt session creation time
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record PendingApprovalVO(
        String sessionId,
        String title,
        String preview,
        String status,
        Instant createdAt
) {
}
