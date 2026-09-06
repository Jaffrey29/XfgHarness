package cn.xiaofuge.deepseek.harness.domain.tool.mcp;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * MCP (Model Context Protocol) client interface.
 * <p>
 * Connects to an external MCP server and exposes its tools.
 * Mirrors TS {@code mcp-client} bridge.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IMcpClient {

    /**
     * Connect to the MCP server.
     *
     * @return future that completes when connected
     */
    CompletableFuture<Void> connect();

    /**
     * Disconnect from the MCP server.
     *
     * @return future that completes when disconnected
     */
    CompletableFuture<Void> disconnect();

    /**
     * Check if currently connected.
     */
    boolean isConnected();

    /**
     * List tools available on the MCP server.
     *
     * @return list of tool descriptors
     */
    CompletableFuture<List<McpToolDescriptor>> listTools();

    /**
     * Call a tool on the MCP server.
     *
     * @param toolName  the raw tool name (not server-qualified)
     * @param arguments tool arguments
     * @return tool result
     */
    CompletableFuture<McpToolResult> callTool(String toolName, Map<String, Object> arguments);

    /**
     * MCP tool descriptor.
     *
     * @param name        tool name
     * @param description tool description
     * @param inputSchema JSON Schema for tool input
     */
    record McpToolDescriptor(
            String name,
            String description,
            Map<String, Object> inputSchema
    ) {}

    /**
     * MCP tool call result.
     *
     * @param content    result content (text or structured)
     * @param isError    whether the result represents an error
     */
    record McpToolResult(
            List<Map<String, Object>> content,
            boolean isError
    ) {}
}
