package cn.walioffice.deepseek.harness.api.service.command;

import cn.walioffice.deepseek.harness.api.dto.goal.GoalCommandRequest;
import cn.walioffice.deepseek.harness.api.response.goal.GoalViewResponse;
import cn.walioffice.deepseek.harness.domain.goal.IGoalCommandService;
import cn.walioffice.deepseek.harness.domain.goal.model.valobj.GoalAction;
import cn.walioffice.deepseek.harness.domain.goal.model.valobj.GoalCommandVO;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * 目标命令适配器。
 * <p>
 * 执行流程：
 * 1. 接收目标动作请求；
 * 2. 转换为领域命令；
 * 3. 返回最新目标状态。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class GoalCommandApi {

    private final IGoalCommandService goalCommandService;

    public GoalCommandApi(IGoalCommandService goalCommandService) {
        this.goalCommandService = goalCommandService;
    }

    /**
     * 执行目标动作。
     * <p>
     * 执行流程：
     * 1. 解析 action；
     * 2. 转换为领域命令；
     * 3. 调用领域服务并映射返回结果。
     */
    public GoalViewResponse apply(String sessionId, GoalCommandRequest request) {
        validateSessionId(sessionId);
        if (request == null) {
            throw new IllegalArgumentException("goal request must not be null");
        }
        if (request.action() == null || request.action().isBlank()) {
            throw new IllegalArgumentException("goal action must not be blank");
        }
        GoalAction action;
        try {
            action = GoalAction.valueOf(request.action().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("unsupported goal action: " + request.action(), exception);
        }
        GoalCommandVO command = new GoalCommandVO(
                action,
                request.goalId(),
                request.title(),
                request.description(),
                request.blockedReason(),
                request.maxRounds(),
                request.continuationEnabled()
        );
        return GoalViewResponse.from(goalCommandService.apply(sessionId, command));
    }

    private static void validateSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
    }
}
