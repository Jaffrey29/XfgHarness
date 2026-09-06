package cn.xiaofuge.deepseek.harness.infrastructure.dao.po;

import java.time.Instant;

/**
 * 持久化目标状态行。
 * <p>
 * 执行流程：
 * 1. 仓储层把聚合拆成 PO；
 * 2. DAO 负责写入数据库；
 * 3. 读取时再映射回领域对象。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record GoalStatePO(
        String sessionId,
        String goalId,
        long revision,
        String title,
        String description,
        String status,
        String blockedReason,
        int maxRounds,
        int roundsStarted,
        boolean continuationEnabled,
        Instant createdAt,
        Instant updatedAt
) {
}
