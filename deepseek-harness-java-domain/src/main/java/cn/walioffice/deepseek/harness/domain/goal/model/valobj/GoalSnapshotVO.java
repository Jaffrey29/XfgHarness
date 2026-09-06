package cn.walioffice.deepseek.harness.domain.goal.model.valobj;

import java.time.Instant;

/**
 * 目标当前快照。
 * <p>
 * 执行流程：
 * 1. 聚合状态变化后生成快照；
 * 2. API 层映射为响应对象；
 * 3. 前端展示当前目标状态。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record GoalSnapshotVO(
        String sessionId,
        String goalId,
        long revision,
        String title,
        String description,
        GoalStatus status,
        String blockedReason,
        int maxRounds,
        int roundsStarted,
        boolean continuationEnabled,
        Instant createdAt,
        Instant updatedAt
) {
}
