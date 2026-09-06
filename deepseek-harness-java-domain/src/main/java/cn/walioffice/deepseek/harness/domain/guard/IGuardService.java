package cn.walioffice.deepseek.harness.domain.guard;

import cn.walioffice.deepseek.harness.domain.guard.model.valobj.GuardDecision;
import java.util.Optional;

/**
 * Application-facing guard seam. Guards observe tool executions and can enrich
 * or block the downstream flow without vetoing the execution itself.
 *
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
