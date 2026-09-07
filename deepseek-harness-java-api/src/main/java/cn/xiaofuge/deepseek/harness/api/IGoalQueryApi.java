package cn.xiaofuge.deepseek.harness.api;

import cn.xiaofuge.deepseek.harness.api.dto.GoalViewResponseDTO;
import cn.xiaofuge.deepseek.harness.api.response.Response;

public interface IGoalQueryApi {

    Response<GoalViewResponseDTO> getGoal(String sessionId);
}
