package cn.walioffice.deepseek.harness.domain.agent.service;

import cn.walioffice.deepseek.harness.domain.agent.model.entity.AgentOptions;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.ToolSchema;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Assembles the system prompt and tool definitions for a model request.
 * Mirrors TS {@code dsh-system-prompt} package's {@code assemble()}.
 * <p>
 * The implementation merges static sections (persona, instructions),
 * dynamic runtime context, and tool schemas into a {@link PromptAssembly}
 * that the agent loop renders into a request.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface SystemPromptAssembler {

    /**
     * Assemble the prompt for one agent step.
     *
     * @param agentId     agent identity for scoped contributions
     * @param options     agent options (provider, model, cwd)
     * @param cwd         working directory for the session
     * @param tools       enabled tool schemas
     * @return the assembled prompt
     */
    CompletableFuture<PromptAssembly> assemble(
            String agentId,
            AgentOptions options,
            String cwd,
            List<ToolSchema> tools
    );

    /**
     * Assembled prompt ready for rendering.
     *
     * @param system rendered system prompt string
     * @param tools  tool schemas to include in the request
     */
    record PromptAssembly(
            String system,
            List<ToolSchema> tools
    ) {}
}
