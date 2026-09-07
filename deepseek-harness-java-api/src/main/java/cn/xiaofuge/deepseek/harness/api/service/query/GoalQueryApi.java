package cn.xiaofuge.deepseek.harness.api.service.query;

import cn.xiaofuge.deepseek.harness.api.response.goal.GoalViewResponse;
import cn.xiaofuge.deepseek.harness.domain.goal.IGoalQueryService;
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
 * <p>调用流程：Controller/DTO → 应用门面 → 用例；响应对象由用例结果转换。</p>
 * <p>示例：DTO 只做协议隔离，不承载领域规则。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class GoalQueryApi {

    private final IGoalQueryService goalQueryService;

    /**
     * 处理目标 Query API。
     * 流程：转换请求 → 调用用例或领域服务 → 转换响应。
     * @param goalQueryService 目标 Query Service参数。
     * @return 方法执行结果。
     */
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

    /**
     * 校验输入与业务规则；不通过时返回失败结果或抛出异常。
     */
    private static void validateSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
    }
}
