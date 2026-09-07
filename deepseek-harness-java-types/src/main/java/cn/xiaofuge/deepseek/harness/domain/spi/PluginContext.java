package cn.xiaofuge.deepseek.harness.domain.spi;

import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolDefinition;

/**
 * Stable registration boundary handed to a Java plugin while it is loading.
 * <p>
 * Registrations are owned by the plugin context. The host reverses them when
 * the plugin is stopped, fails to load, or is unloaded.
 */
public interface PluginContext {

    String pluginId();

    /**
     * Register one tool with the host. The returned disposer is managed by the
     * host and is invoked in reverse registration order.
     */
    AutoCloseable registerTool(ToolDefinition tool);

    /**
     * Register a resource that must be released before the plugin classloader
     * is closed. This is useful for connections, timers, or file watchers.
     */
    void registerDisposer(AutoCloseable disposer);
}
