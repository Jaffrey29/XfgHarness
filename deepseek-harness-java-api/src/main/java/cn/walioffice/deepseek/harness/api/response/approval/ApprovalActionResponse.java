package cn.walioffice.deepseek.harness.api.response.approval;

import java.time.Instant;

/**
 * 返回审批中心一次操作的结果。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record ApprovalActionResponse(
        String sessionId,
        String status,
        String message,
        Instant operatedAt
) {
}
