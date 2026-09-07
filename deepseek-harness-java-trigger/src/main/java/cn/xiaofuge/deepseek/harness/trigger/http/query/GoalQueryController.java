package cn.xiaofuge.deepseek.harness.trigger.http.query;

import cn.xiaofuge.deepseek.harness.api.response.goal.GoalViewResponse;
import cn.xiaofuge.deepseek.harness.api.service.query.GoalQueryApi;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 目标查询控制器。
 * <p>
 * 执行流程：
 * 1. 接收前端查询请求；
 * 2. 调用查询适配器读取当前目标；
 * 3. 直接返回响应体。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@RestController
@RequestMapping("/api/harness/goals")
public class GoalQueryController {

    private final GoalQueryApi goalQueryApi;

    /**
     * 处理Goal Query Controller相关逻辑。
     */
    public GoalQueryController(GoalQueryApi goalQueryApi) {
        this.goalQueryApi = goalQueryApi;
    }

    /**
     * 查询会话当前目标。
     * <p>
     * 执行流程：
     * 1. 接收 sessionId；
     * 2. 查询目标快照；
     * 3. 返回给前端。
     */
    @GetMapping("/{sessionId}")
    public GoalViewResponse getGoal(@PathVariable("sessionId") String sessionId) {
        return goalQueryApi.getGoal(sessionId);
    }
}
