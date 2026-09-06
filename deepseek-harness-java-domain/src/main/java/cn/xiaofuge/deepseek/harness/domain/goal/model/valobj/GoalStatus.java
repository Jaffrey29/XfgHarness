package cn.xiaofuge.deepseek.harness.domain.goal.model.valobj;

/**
 * 目标当前状态。
 * <p>
 * 执行流程：
 * 1. 聚合根据动作切换状态；
 * 2. 仓储持久化最新快照；
 * 3. 查询接口直接读取当前状态。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public enum GoalStatus {
    ACTIVE,
    PAUSED,
    BLOCKED,
    COMPLETED,
    CLEARED;

    /**
     * 判断是否属于终态。
     * <p>
     * 执行流程：
     * 1. 读取当前状态；
     * 2. 判断是否已结束；
     * 3. 返回布尔结果。
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == CLEARED;
    }
}
