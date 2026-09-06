package cn.walioffice.deepseek.harness.domain.tool.mcp;

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
 * Adapter that wraps an MCP tool as a {@link ToolDefinition}.
 * <p>
 * Tool name is server-qualified: {@code mcp__<serverName>__<rawName>}.
 * Mirrors TS MCP tool registration pattern.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class McpToolAdapter implements ToolDefinition {

    private final String serverName;
    private final IMcpClient.McpToolDescriptor descriptor;
    private final IMcpClient mcpClient;
    private final long timeoutMs;

    public McpToolAdapter(String serverName, IMcpClient.McpToolDescriptor descriptor, IMcpClient mcpClient, long timeoutMs) {
        this.serverName = serverName;
        this.descriptor = descriptor;
        this.mcpClient = mcpClient;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public String name() {
        return "mcp__" + serverName + "__" + descriptor.name();
    }

    @Override
    public String description() {
        return descriptor.description() != null ? descriptor.description() : "MCP tool: " + descriptor.name();
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

        return mcpClient.callTool(descriptor.name(), argMap)
                .thenApply(result -> {
                    if (result.isError()) {
                        String errorText = result.content().stream()
                                .filter(c -> "text".equals(c.get("type")))
                                .map(c -> String.valueOf(c.get("text")))
                                .reduce("", (a, b) -> a + b);
                        return ToolExecutionResult.fail(
                                errorText.isEmpty() ? "MCP tool error" : errorText,
                                "MCP_ERROR");
                    }

                    List<ContentBlock> blocks = new ArrayList<>();
                    for (Map<String, Object> c : result.content()) {
                        if ("text".equals(c.get("type"))) {
                            blocks.add(new TextBlock(String.valueOf(c.get("text"))));
                        }
                    }

                    if (blocks.isEmpty()) {
                        blocks.add(new TextBlock("[MCP tool returned no text content]"));
                    }

                    return (ToolExecutionResult) ToolExecutionResult.ok(blocks);
                })
                .exceptionally(e -> ToolExecutionResult.fail(
                        "MCP call failed: " + e.getMessage(), "MCP_CALL_ERROR"));
    }

    @Override
    public boolean isConcurrencySafe(Object args) { return true; }

    @Override
    public long timeoutMs() { return timeoutMs; }
}
