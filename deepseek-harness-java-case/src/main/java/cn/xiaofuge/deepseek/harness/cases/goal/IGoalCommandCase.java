package cn.xiaofuge.deepseek.harness.cases.goal;

import cn.xiaofuge.deepseek.harness.api.dto.GoalViewResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.GoalCommandRequestDTO;

/**
 * 目标命令用例契约。
 * <p>流程：Trigger → GoalCommandCase → Pipeline → GoalCommandNode → 领域目标服务。</p>
 * <p>数据示例：{@code sessionId=session-001, action=CREATE, title="修复登录"}
 * 创建目标；{@code action=BLOCK} 挂起目标。</p>
 */
public interface IGoalCommandCase {
    GoalViewResponseDTO apply(String sessionId, GoalCommandRequestDTO request);
}
