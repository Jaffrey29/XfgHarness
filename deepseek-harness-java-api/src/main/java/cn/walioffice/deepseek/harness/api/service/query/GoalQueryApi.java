package cn.walioffice.deepseek.harness.api.service.query;

import cn.walioffice.deepseek.harness.api.response.goal.GoalViewResponse;
import cn.walioffice.deepseek.harness.domain.goal.IGoalQueryService;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * 目标查询适配器。
 * <p>
 * 执行流程：
 * 1. 接收目标查询请求；
 * 2. 调用领域查询服务；
 * 3. 转换为展示响应。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class GoalQueryApi {

    private final IGoalQueryService goalQueryService;

    public GoalQueryApi(IGoalQueryService goalQueryService) {
        this.goalQueryService = goalQueryService;
    }

    /**
     * 查询当前目标。
     * <p>
     * 执行流程：
     * 1. 读取 sessionId；
     * 2. 查询领域目标快照；
     * 3. 返回可展示结果。
     */
    public GoalViewResponse getGoal(String sessionId) {
        validateSessionId(sessionId);
        Optional<GoalViewResponse> response = goalQueryService.getGoal(sessionId).map(GoalViewResponse::from);
        return response.orElseGet(() -> GoalViewResponse.empty(sessionId));
    }

    private static void validateSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
    }
}
