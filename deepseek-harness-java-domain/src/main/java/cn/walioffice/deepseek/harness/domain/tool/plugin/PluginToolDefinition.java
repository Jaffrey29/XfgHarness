package cn.walioffice.deepseek.harness.domain.tool.plugin;

import cn.walioffice.deepseek.harness.domain.llm.model.valobj.ContentBlock;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.TextBlock;
import cn.walioffice.deepseek.harness.domain.tool.model.entity.ToolDefinition;
import cn.walioffice.deepseek.harness.domain.tool.model.entity.ToolExecutionResult;
import cn.walioffice.deepseek.harness.domain.tool.model.entity.ToolRunContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Adapts a plugin sidecar tool as a {@link ToolDefinition}.
 * <p>
 * Tool name is plugin-qualified: {@code plugin__<pluginId>__<rawName>}.
 * When invoked, delegates execution to the {@link IPluginToolBridge}.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class PluginToolDefinition implements ToolDefinition {

    private final String pluginId;
    private final IPluginToolBridge bridge;
    private final IPluginToolBridge.PluginToolDescriptor descriptor;

    public PluginToolDefinition(
            String pluginId,
            IPluginToolBridge bridge,
            IPluginToolBridge.PluginToolDescriptor descriptor) {
        this.pluginId = pluginId;
        this.bridge = bridge;
        this.descriptor = descriptor;
    }

    @Override
    public String name() {
        return "plugin__" + pluginId + "__" + descriptor.name();
    }

    @Override
    public String description() {
        return descriptor.description() != null ? descriptor.description() : "Plugin tool: " + descriptor.name();
    }

    @Override
    public Map<String, Object> parameters() {
        return descriptor.inputSchema() != null ? descriptor.inputSchema() : Map.of("type", "object");
    }

    @Override
    public Map<String, Object> outputSchema() {
        return Map.of("type", "object", "additionalProperties", true);
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<ToolExecutionResult> execute(Object args, ToolRunContext ctx) {
        Map<String, Object> argMap = args instanceof Map ? (Map<String, Object>) args : Map.of();

        return bridge.callTool(pluginId, descriptor.name(), argMap)
                .thenApply(result -> {
                    if (result.isError()) {
                        return ToolExecutionResult.fail(
                                result.errorText() != null ? result.errorText() : "Plugin tool error",
                                "PLUGIN_ERROR");
                    }

                    List<ContentBlock> blocks = result.content();
                    if (blocks == null || blocks.isEmpty()) {
                        blocks = List.of(new TextBlock("[Plugin tool returned no content]"));
                    }

                    return (ToolExecutionResult) ToolExecutionResult.ok(blocks);
                })
                .exceptionally(e -> ToolExecutionResult.fail(
                        "Plugin tool call failed: " + e.getMessage(), "PLUGIN_CALL_ERROR"));
    }

    @Override
    public boolean isConcurrencySafe(Object args) { return true; }
}
