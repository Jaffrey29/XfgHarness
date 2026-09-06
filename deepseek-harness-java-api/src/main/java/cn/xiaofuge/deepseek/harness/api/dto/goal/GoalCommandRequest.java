package cn.xiaofuge.deepseek.harness.api.dto.goal;

/**
 * 目标变更请求。
 * <p>
 * 执行流程：
 * 1. 控制器接收请求体；
 * 2. API 层转换为领域命令；
 * 3. 领域服务执行状态机。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record GoalCommandRequest(
        String action,
        String goalId,
        String title,
        String description,
        String blockedReason,
        Integer maxRounds,
        Boolean continuationEnabled
) {
}
