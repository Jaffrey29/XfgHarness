package cn.xiaofuge.deepseek.harness.trigger.http.command;

import cn.xiaofuge.deepseek.harness.api.dto.goal.GoalCommandRequest;
import cn.xiaofuge.deepseek.harness.api.response.goal.GoalViewResponse;
import cn.xiaofuge.deepseek.harness.api.service.command.GoalCommandApi;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 目标命令控制器。
 * <p>
 * 执行流程：
 * 1. 接收前端动作请求；
 * 2. 调用命令适配器推进目标状态；
 * 3. 返回最新目标快照。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@RestController
@RequestMapping("/api/harness/goals")
public class GoalCommandController {

    private final GoalCommandApi goalCommandApi;

    /**
     * 处理Goal Command Controller相关逻辑。
     */
    public GoalCommandController(GoalCommandApi goalCommandApi) {
        this.goalCommandApi = goalCommandApi;
    }

    /**
     * 执行目标动作。
     * <p>
     * 执行流程：
     * 1. 接收 sessionId 与动作请求体；
     * 2. 调用领域命令服务；
     * 3. 返回更新后的目标状态。
     */
    @PostMapping("/{sessionId}")
    public GoalViewResponse apply(@PathVariable("sessionId") String sessionId, @RequestBody GoalCommandRequest request) {
        return goalCommandApi.apply(sessionId, request);
    }
}
