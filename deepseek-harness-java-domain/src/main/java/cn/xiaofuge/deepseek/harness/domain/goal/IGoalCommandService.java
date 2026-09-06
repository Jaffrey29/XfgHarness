package cn.xiaofuge.deepseek.harness.domain.goal;

import cn.xiaofuge.deepseek.harness.domain.goal.model.valobj.GoalCommandVO;
import cn.xiaofuge.deepseek.harness.domain.goal.model.valobj.GoalSnapshotVO;

/**
 * 目标命令服务接口。
 * <p>
 * 执行流程：
 * 1. 上层传入 sessionId 与动作命令；
 * 2. 领域层驱动目标状态机；
 * 3. 返回最新目标快照。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IGoalCommandService {

    /**
     * 执行目标动作。
     * <p>
     * 执行流程：
     * 1. 读取当前目标；
     * 2. 按动作更新聚合；
     * 3. 持久化并返回最新快照。
     */
    GoalSnapshotVO apply(String sessionId, GoalCommandVO command);
}
