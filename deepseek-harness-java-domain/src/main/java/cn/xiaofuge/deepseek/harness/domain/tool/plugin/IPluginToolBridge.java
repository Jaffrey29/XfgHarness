package cn.xiaofuge.deepseek.harness.domain.tool.plugin;

import cn.xiaofuge.deepseek.harness.domain.model.valobj.ContentBlock;

import java.util.*;
import java.util.concurrent.*;

/**
 * Domain-layer port for communicating with a plugin sidecar process.
 * <p>
 * Defines the contract for the plugin→tool bridge: the infrastructure layer
 * implements this to talk to a real Node sidecar; tests can mock it.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IPluginToolBridge {

    /**
     * Connect to the sidecar and perform handshake.
     *
     * @param pluginId  plugin identifier
     * @param process   the already-started sidecar process
     * @return future completed when handshake is done
     */
    CompletableFuture<Void> connect(String pluginId, Process process);

    /** Disconnect and clean up. */
    CompletableFuture<Void> disconnect(String pluginId);

    /** Whether the sidecar is connected. */
    boolean isConnected(String pluginId);

    /**
     * List tools declared by the sidecar.
     *
     * @return list of tool descriptors
     */
    CompletableFuture<List<PluginToolDescriptor>> listTools(String pluginId);

    /**
     * Call a tool on the sidecar.
     *
     * @param toolName  raw tool name
     * @param arguments tool arguments
     * @return tool result content
     */
    CompletableFuture<PluginToolResult> callTool(String pluginId, String toolName, Map<String, Object> arguments);

    /**
     * Tool descriptor from a plugin sidecar.
     *
     * @param name        tool name
     * @param description tool description
     * @param inputSchema JSON Schema for input parameters
     */
    record PluginToolDescriptor(
            String name,
            String description,
            Map<String, Object> inputSchema
    ) {}

    /**
     * Tool call result from a plugin sidecar.
     *
     * @param content  result content blocks
     * @param isError  whether this is an error result
     */
    record PluginToolResult(
            List<ContentBlock> content,
            boolean isError,
            String errorText
    ) {}
}
