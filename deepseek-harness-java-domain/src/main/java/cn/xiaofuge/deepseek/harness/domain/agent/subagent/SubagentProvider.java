package cn.xiaofuge.deepseek.harness.domain.agent.subagent;

import cn.xiaofuge.deepseek.harness.domain.agent.model.entity.AgentOptions;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.ContentBlock;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.ToolSchema;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Port for subagent providers.
 * <p>
 * Mirrors TS {@code SubagentProvider}: a provider creates and runs a
 * child agent, returning its final output. Implementations may be
 * in-process (fork) or out-of-process (ACP, subprocess).
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface SubagentProvider {

    /** Provider name for registration. */
    String name();

    /**
     * Start a subagent run.
     *
     * @param request the start request
     * @return the run result
     */
    CompletableFuture<SubagentResult> start(SubagentStartRequest request);

    /** Capabilities advertised at start time. */
    SubagentCapabilities capabilities();

    // ── Types ───────────────────────────────────────────────────

    /**
     * SubagentStartRequest 定义当前模块中的record契约与职责。
     */
    record SubagentStartRequest(
            String prompt,
            AgentOptions options,
            String parentSessionId,
            String parentCwd,
            int delegationDepth,
            List<ToolSchema> enabledTools,
            List<String> allowedToolNames
    ) {}

    /**
     * SubagentCapabilities 定义当前模块中的record契约与职责。
     */
    record SubagentCapabilities(
            boolean continuable,
            boolean structuredOutput,
            boolean localAgent
    ) {}

    /**
     * SubagentResult 定义当前模块中的record契约与职责。
     */
    record SubagentResult(
            String stopReason,      // "completed" | "aborted" | "error" | "max-tokens"
            List<ContentBlock> output,
            String runId
    ) {}
}
