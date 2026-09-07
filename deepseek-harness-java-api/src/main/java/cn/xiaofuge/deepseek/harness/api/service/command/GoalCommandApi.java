package cn.xiaofuge.deepseek.harness.api.service.command;

import cn.xiaofuge.deepseek.harness.api.dto.goal.GoalCommandRequest;
import cn.xiaofuge.deepseek.harness.api.response.goal.GoalViewResponse;
import cn.xiaofuge.deepseek.harness.domain.goal.IGoalCommandService;
import cn.xiaofuge.deepseek.harness.domain.goal.model.valobj.GoalAction;
import cn.xiaofuge.deepseek.harness.domain.goal.model.valobj.GoalCommandVO;
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
 * <p>调用流程：Controller/DTO → 应用门面 → 用例；响应对象由用例结果转换。</p>
 * <p>示例：DTO 只做协议隔离，不承载领域规则。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class GoalCommandApi {

    private final IGoalCommandService goalCommandService;

    /**
     * 处理目标 命令 API。
     * 流程：转换请求 → 调用用例或领域服务 → 转换响应。
     * @param goalCommandService 目标 命令 Service参数。
     * @return 方法执行结果。
     */
    public GoalCommandApi(IGoalCommandService goalCommandService) {
        this.goalCommandService = goalCommandService;
    }

    /**
     * 处理apply。
     * 流程：转换请求 → 调用用例或领域服务 → 转换响应。
     * @param sessionId 会话 ID。
     * @param request 请求对象。
     * @return 方法执行结果。
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

    /**
     * 校验输入与业务规则；不通过时返回失败结果或抛出异常。
     */
    private static void validateSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
    }
}
