package cn.xiaofuge.deepseek.harness.domain.goal.model.valobj;

/**
 * 目标变更动作。
 * <p>
 * 执行流程：
 * 1. API 层接收动作字符串；
 * 2. 领域层映射为受控枚举；
 * 3. 聚合根据动作推进状态机。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public enum GoalAction {
    CREATE,
    UPDATE,
    PAUSE,
    RESUME,
    COMPLETE,
    BLOCK,
    CLEAR
}
