package cn.xiaofuge.deepseek.harness.api;

import cn.xiaofuge.deepseek.harness.api.dto.GoalCommandRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.GoalViewResponseDTO;
import cn.xiaofuge.deepseek.harness.api.response.Response;

public interface IGoalCommandApi {

    Response<GoalViewResponseDTO> apply(String sessionId, GoalCommandRequestDTO request);
}
