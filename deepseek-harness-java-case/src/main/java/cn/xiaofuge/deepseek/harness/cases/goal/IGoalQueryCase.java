package cn.xiaofuge.deepseek.harness.cases.goal;

import cn.xiaofuge.deepseek.harness.api.dto.GoalViewResponseDTO;

public interface IGoalQueryCase {
    GoalViewResponseDTO getGoal(String sessionId);
}
