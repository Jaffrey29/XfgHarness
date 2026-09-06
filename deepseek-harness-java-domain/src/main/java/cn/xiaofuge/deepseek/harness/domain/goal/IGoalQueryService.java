package cn.xiaofuge.deepseek.harness.domain.goal;

import cn.xiaofuge.deepseek.harness.domain.goal.model.valobj.GoalSnapshotVO;
import java.util.Optional;

/**
 * 目标查询服务接口。
 * <p>
 * 执行流程：
 * 1. 上层传入 sessionId；
 * 2. 领域层读取当前目标快照；
 * 3. 返回可选结果给调用方。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IGoalQueryService {

    /**
     * 查询当前目标。
     * <p>
     * 执行流程：
     * 1. 定位会话；
     * 2. 读取目标快照；
     * 3. 返回可选结果。
     */
    Optional<GoalSnapshotVO> getGoal(String sessionId);
}
