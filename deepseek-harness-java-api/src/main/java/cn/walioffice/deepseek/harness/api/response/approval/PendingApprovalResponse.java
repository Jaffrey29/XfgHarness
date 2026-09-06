package cn.walioffice.deepseek.harness.api.response.approval;

import java.time.Instant;

/**
 * 返回 Web 控制台使用的一张待审批卡片。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record PendingApprovalResponse(
        String sessionId,
        String title,
        String preview,
        String status,
        Instant createdAt
) {
}
