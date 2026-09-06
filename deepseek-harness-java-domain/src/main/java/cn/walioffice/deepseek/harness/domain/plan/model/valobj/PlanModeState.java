package cn.walioffice.deepseek.harness.domain.plan.model.valobj;

/**
 * 计划模式状态。
 * <p>
 * 执行流程：
 * 1. 会话进入计划阶段时切换到 `Planning`；
 * 2. 用户提交计划后切换到 `Approved`；
 * 3. 默认回到 `Off`。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public sealed interface PlanModeState permits
        PlanModeState.Off,
        PlanModeState.Planning,
        PlanModeState.Approved {

    /** 正常模式。 */
    record Off() implements PlanModeState {}

    /** 计划中。 */
    record Planning() implements PlanModeState {}

    /** 计划已提交等待批准。 */
    record Approved(String plan) implements PlanModeState {}
}
