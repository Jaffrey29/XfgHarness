package cn.xiaofuge.deepseek.harness.domain.task.approval.model.valobj;

import java.time.Instant;

/**
 * ApprovalActionVO 表示ApprovalAction的领域值对象。
 */
public record ApprovalActionVO(
        String sessionId,
        String status,
        String message,
        Instant operatedAt
) {
}
