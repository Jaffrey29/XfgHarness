package cn.xiaofuge.deepseek.harness.domain.task.approval.model.valobj;

import java.time.Instant;

/**
 * Represents the result of one approval action.
 *
 * @param sessionId session identifier
 * @param status current session status after the action
 * @param message user-facing action result
 * @param operatedAt action timestamp
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record ApprovalActionVO(
        String sessionId,
        String status,
        String message,
        Instant operatedAt
) {
}
