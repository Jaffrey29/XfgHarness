package cn.xiaofuge.deepseek.harness.trigger.service.command;

import cn.xiaofuge.deepseek.harness.api.dto.GoalCommandRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.GoalViewResponseDTO;
import cn.xiaofuge.deepseek.harness.api.IGoalCommandApi;
import cn.xiaofuge.deepseek.harness.api.response.Response;
import cn.xiaofuge.deepseek.harness.cases.goal.IGoalCommandCase;
import org.springframework.stereotype.Service;

@Service
public class GoalCommandApi implements IGoalCommandApi {
    private final IGoalCommandCase goalCommandCase;

    public GoalCommandApi(IGoalCommandCase goalCommandCase) {
        this.goalCommandCase = goalCommandCase;
    }

    public Response<GoalViewResponseDTO> apply(String sessionId, GoalCommandRequestDTO request) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
        if (request == null || request.action() == null || request.action().isBlank()) {
            throw new IllegalArgumentException("goal action must not be blank");
        }
        return execute(() -> goalCommandCase.apply(sessionId, request));
    }

    private <T> Response<T> execute(java.util.function.Supplier<T> action) {
        try {
            return Response.success(action.get());
        } catch (IllegalArgumentException exception) {
            return Response.invalidArgument(exception.getMessage());
        } catch (RuntimeException exception) {
            return Response.internalError(exception.getMessage());
        }
    }
}
