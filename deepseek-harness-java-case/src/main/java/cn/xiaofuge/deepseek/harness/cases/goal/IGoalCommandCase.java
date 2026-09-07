package cn.xiaofuge.deepseek.harness.cases.goal;

import cn.xiaofuge.deepseek.harness.api.dto.GoalViewResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.GoalCommandRequestDTO;

public interface IGoalCommandCase {
    GoalViewResponseDTO apply(String sessionId, GoalCommandRequestDTO request);
}
