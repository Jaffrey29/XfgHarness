package cn.xiaofuge.deepseek.harness.domain.guard;

import cn.xiaofuge.deepseek.harness.domain.guard.model.valobj.GuardDecision;
import java.util.Optional;

/**
 * Application-facing guard seam. Guards observe tool executions and can enrich
 * or block the downstream flow without vetoing the execution itself.
 *
 * <p>调用流程：应用领域规则 → 更新聚合或值对象 → 发布事件/返回结果。</p>
 * <p>示例：领域对象只暴露业务语言方法，不直接依赖基础设施。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IGuardService {

    /**
     * Observe a completed tool call and return a decision.
     *
     * @param agentId    the agent making the call (empty for direct callers)
     * @param toolName   tool name
     * @param arguments  canonical (JSON-stringified) arguments
     * @param resultJson the tool's result as JSON
     * @return guard decision with optional reminder
     */
    GuardDecision postExecute(Optional<String> agentId, String toolName, String arguments, String resultJson);

    /**
     * Called before each agent step; a user interjection resets repeat tracking.
     *
     * @param agentId    the agent
     * @param hasUserMessage whether the incoming messages contain a user message
     */
    void preStep(String agentId, boolean hasUserMessage);
}
