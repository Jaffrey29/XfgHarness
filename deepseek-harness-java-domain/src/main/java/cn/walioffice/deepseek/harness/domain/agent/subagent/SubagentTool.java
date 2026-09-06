package cn.walioffice.deepseek.harness.domain.agent.subagent;

import cn.walioffice.deepseek.harness.domain.llm.model.valobj.ContentBlock;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.TextBlock;
import cn.walioffice.deepseek.harness.domain.tool.model.entity.ToolDefinition;
import cn.walioffice.deepseek.harness.domain.tool.model.entity.ToolExecutionResult;
import cn.walioffice.deepseek.harness.domain.tool.model.entity.ToolRunContext;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Model-facing tool that delegates work to a subagent provider.
 * <p>
 * Corresponds to TS {@code tool-subagent}. The model invokes this tool with
 * a prompt and optional configuration; the tool starts a child agent via
 * the configured {@link SubagentProvider}, waits for completion, and returns
 * the child's final assistant output as the tool result.
 * <p>
 * Default tool name is {@code subagent}; each loaded instance must use a
 * distinct name.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class SubagentTool implements ToolDefinition {

    private static final String DEFAULT_TOOL_NAME = "subagent";

    private final String toolName;
    private final SubagentRegistry registry;
    private final String providerName;

    public SubagentTool(
            SubagentRegistry registry,
            String providerName
    ) {
        this(registry, providerName, DEFAULT_TOOL_NAME);
    }

    public SubagentTool(
            SubagentRegistry registry,
            String providerName,
            String toolName
    ) {
        this.registry = registry;
        this.providerName = providerName;
        this.toolName = toolName;
    }

    @Override
    public String name() {
        return toolName;
    }

    @Override
    public String description() {
        return """
                Delegate a task to a subagent (child agent). The subagent runs in its own \
                session with its own context and tools. Use this for complex subtasks that \
                benefit from focused multi-step reasoning. The subagent's final answer is \
                returned as this tool's result.""";
    }

    @Override
    public Map<String, Object> parameters() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();

        Map<String, Object> promptProp = new LinkedHashMap<>();
        promptProp.put("type", "string");
        promptProp.put("description", "The task description / prompt for the subagent.");
        properties.put("prompt", promptProp);

        Map<String, Object> bgProp = new LinkedHashMap<>();
        bgProp.put("type", "boolean");
        bgProp.put("description", "If true, run the subagent in the background (default: false).");
        properties.put("run_in_background", bgProp);

        props.put("properties", properties);
        props.put("required", List.of("prompt"));

        return props;
    }

    @Override
    public Map<String, Object> outputSchema() {
        return Map.of();
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<ToolExecutionResult> execute(Object args, ToolRunContext exec) {
        Map<String, Object> argMap;
        if (args instanceof Map) {
            argMap = (Map<String, Object>) args;
        } else {
            return CompletableFuture.completedFuture(
                    ToolExecutionResult.fail("Invalid args type", "BAD_ARGS")
            );
        }

        String prompt = argMap.get("prompt") instanceof String s ? s : "";
        if (prompt.isEmpty()) {
            return CompletableFuture.completedFuture(
                    ToolExecutionResult.fail("prompt is required", "MISSING_PROMPT")
            );
        }

        SubagentProvider provider = registry.resolve(providerName);

        SubagentProvider.SubagentStartRequest request = new SubagentProvider.SubagentStartRequest(
                prompt,
                null,           // options (use provider defaults)
                null,           // parentSessionId
                null,           // parentCwd
                0,              // delegationDepth (top-level)
                List.of(),      // enabledTools (all)
                List.of()       // allowedToolNames (all)
        );

        return provider.start(request).<ToolExecutionResult>thenApply(result -> {
            List<ContentBlock> content;
            if (result.output() != null && !result.output().isEmpty()) {
                content = result.output();
            } else {
                content = List.of(new TextBlock(
                        "[subagent ended with reason: " + result.stopReason() + " and no output]"
                ));
            }

            if ("error".equals(result.stopReason()) || "aborted".equals(result.stopReason())) {
                return ToolExecutionResult.fail(
                        "Subagent ended with reason: " + result.stopReason(),
                        "SUBAGENT_" + result.stopReason().toUpperCase()
                );
            }

            return ToolExecutionResult.ok(content);
        }).exceptionally(e -> ToolExecutionResult.fail(
                "Subagent execution failed: " + e.getMessage(),
                "SUBAGENT_ERROR"
        ));
    }

    @Override
    public boolean isConcurrencySafe(Object args) {
        // Subagent runs are exclusive — they create child agents
        return false;
    }

    @Override
    public long timeoutMs() {
        return 300_000; // 5 minutes default
    }
}
