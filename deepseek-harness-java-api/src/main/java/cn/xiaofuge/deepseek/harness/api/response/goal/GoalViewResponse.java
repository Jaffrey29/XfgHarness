package cn.xiaofuge.deepseek.harness.api.response.goal;

import cn.xiaofuge.deepseek.harness.domain.goal.model.valobj.GoalSnapshotVO;
import java.time.Instant;

/**
 * 目标展示响应。
 * <p>
 * 执行流程：
 * 1. API 层读取领域快照；
 * 2. 组装为前端可展示对象；
 * 3. 返回当前目标状态。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record GoalViewResponse(
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
        Instant updatedAt,
        boolean exists
) {

    /**
     * 从领域快照构建响应。
     * <p>
     * 执行流程：
     * 1. 读取领域快照；
     * 2. 映射为 API 响应；
     * 3. 设置存在标记。
     */
    public static GoalViewResponse from(GoalSnapshotVO snapshot) {
        return new GoalViewResponse(
                snapshot.sessionId(),
                snapshot.goalId(),
                snapshot.revision(),
                snapshot.title(),
                snapshot.description(),
                snapshot.status().name(),
                snapshot.blockedReason(),
                snapshot.maxRounds(),
                snapshot.roundsStarted(),
                snapshot.continuationEnabled(),
                snapshot.createdAt(),
                snapshot.updatedAt(),
                true
        );
    }

    /**
     * 构建一个空响应。
     * <p>
     * 执行流程：
     * 1. 读取 sessionId；
     * 2. 其余字段保持为空或零值；
     * 3. 标记当前没有目标。
     */
    public static GoalViewResponse empty(String sessionId) {
        return new GoalViewResponse(sessionId, null, 0, null, null, null, null, 0, 0, false, null, null, false);
    }
}
