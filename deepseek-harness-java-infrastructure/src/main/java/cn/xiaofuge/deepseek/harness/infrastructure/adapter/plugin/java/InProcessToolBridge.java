package cn.xiaofuge.deepseek.harness.infrastructure.adapter.plugin.java;

import cn.xiaofuge.deepseek.harness.domain.model.valobj.ContentBlock;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.TextBlock;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolDefinition;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolExecution;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolExecutionResult;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolRunContext;
import cn.xiaofuge.deepseek.harness.domain.tool.plugin.IPluginToolBridge;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Adapts an in-process {@link ToolDefinition} (loaded from a Java plugin JAR)
 * to the {@link IPluginToolBridge} contract so that {@link PluginToolDefinition}
 * can wrap it uniformly.
 * <p>
 * Unlike {@code JsonRpcPluginToolBridge} which talks to a Node sidecar over
 * stdio, this bridge delegates directly to the in-JVM tool implementation —
 * no serialization, no subprocess.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
class InProcessToolBridge implements IPluginToolBridge {

    private final ToolDefinition delegate;

    InProcessToolBridge(ToolDefinition delegate) {
        this.delegate = delegate;
    }

    @Override
    public CompletableFuture<Void> connect(String pluginId, Process process) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> disconnect(String pluginId) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public boolean isConnected(String pluginId) {
        return true;
    }

    @Override
    public CompletableFuture<List<PluginToolDescriptor>> listTools(String pluginId) {
        return CompletableFuture.completedFuture(List.of(
                new PluginToolDescriptor(delegate.name(), delegate.description(), delegate.parameters())
        ));
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<PluginToolResult> callTool(String pluginId, String toolName, Map<String, Object> arguments) {
        ToolExecution exec = new ToolExecution(
                java.util.UUID.randomUUID().toString(),
                java.util.UUID.randomUUID().toString(),
                toolName,
                arguments,
                java.util.UUID.randomUUID().toString()
        );
        ToolRunContext ctx = new ToolRunContext(exec);

        return delegate.execute(arguments, ctx)
                .thenApply(result -> {
                    List<ContentBlock> content = result.content();
                    if (content == null || content.isEmpty()) {
                        content = List.of(new TextBlock("[tool returned no content]"));
                    }
                    if (result.isError()) {
                        ToolExecutionResult.Failure failure = (ToolExecutionResult.Failure) result;
                        String errorText = failure.error() != null ? failure.error().message() : "tool error";
                        return new PluginToolResult(content, true, errorText);
                    }
                    return new PluginToolResult(content, false, null);
                })
                .exceptionally(e -> new PluginToolResult(
                        List.of(new TextBlock("tool call failed: " + e.getMessage())),
                        true, e.getMessage()));
    }
}
