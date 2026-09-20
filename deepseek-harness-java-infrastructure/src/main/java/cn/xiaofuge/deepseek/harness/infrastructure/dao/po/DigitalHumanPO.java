package cn.xiaofuge.deepseek.harness.infrastructure.dao.po;

import java.time.Instant;

/**
 * 数字人身份记录：名称、头像、用途、能力与审批策略。
 * 与连接配置（DigitalHumanEndpointPO）解耦，便于同一身份切换端点。
 */
public record DigitalHumanPO(
        String id,
        String displayName,
        String avatarRef,
        String purpose,
        String roleTags,
        String themeColor,
        String approvalPolicy,
        Integer concurrencyLimit,
        String projectPath,
        Instant createdAt,
        Instant updatedAt
) {
}
