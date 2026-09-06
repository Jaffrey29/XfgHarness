package cn.walioffice.deepseek.harness.domain.goal.model.valobj;

/**
 * 目标变更命令值对象。
 * <p>
 * 执行流程：
 * 1. API 层把请求体转换为命令；
 * 2. 领域服务执行参数校验；
 * 3. 聚合完成状态切换。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record GoalCommandVO(
        GoalAction action,
        String goalId,
        String title,
        String description,
        String blockedReason,
        Integer maxRounds,
        Boolean continuationEnabled
) {
}
