package cn.xiaofuge.deepseek.harness.infrastructure.adapter.agent;

import cn.xiaofuge.deepseek.harness.domain.agent.model.entity.AgentOptions;
import cn.xiaofuge.deepseek.harness.domain.agent.service.SystemPromptAssembler;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.ToolSchema;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Default {@link SystemPromptAssembler} implementation.
 * <p>
 * Produces a minimal system prompt with agent identity, working directory,
 * and available tools. This is intentionally simple — the TS version merges
 * contributions from the {@code dsh-system-prompt} plugin; this Java default
 * can be replaced by a richer implementation later.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class DefaultSystemPromptAssembler implements SystemPromptAssembler {

    @Override
    public CompletableFuture<PromptAssembly> assemble(
            String agentId,
            AgentOptions options,
            String cwd,
            List<ToolSchema> tools
    ) {
        StringBuilder sb = new StringBuilder();

        sb.append("You are ").append(agentId).append(", an AI assistant.\n\n");
        sb.append("## Environment\n\n");
        sb.append("- Provider: ").append(options.provider()).append('\n');
        sb.append("- Model: ").append(options.model()).append('\n');
        sb.append("- Working directory: ").append(cwd).append('\n');

        if (tools != null && !tools.isEmpty()) {
            sb.append("\n## Available Tools\n\n");
            for (ToolSchema tool : tools) {
                sb.append("- **").append(tool.name()).append("**");
                if (tool.description() != null && !tool.description().isBlank()) {
                    sb.append(": ").append(tool.description());
                }
                sb.append('\n');
            }
        }

        sb.append("\n## Instructions\n\n");
        sb.append("Respond helpfully and concisely. When a task requires tool usage, ");
        sb.append("use the available tools. When the task is complete, stop calling tools ");
        sb.append("and provide a final response.\n");

        return CompletableFuture.completedFuture(new PromptAssembly(sb.toString(), tools));
    }
}
