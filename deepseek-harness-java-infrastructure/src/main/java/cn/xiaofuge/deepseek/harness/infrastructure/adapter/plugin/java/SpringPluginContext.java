package cn.xiaofuge.deepseek.harness.infrastructure.adapter.plugin.java;

import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolDefinition;
import cn.xiaofuge.deepseek.harness.domain.spi.PluginContext;
import cn.xiaofuge.deepseek.harness.domain.tool.adapter.ToolRegistry;
import cn.xiaofuge.deepseek.harness.domain.tool.plugin.IPluginToolBridge;
import cn.xiaofuge.deepseek.harness.domain.tool.plugin.PluginToolDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class SpringPluginContext implements PluginContext, AutoCloseable {

    private final String pluginId;
    private final ToolRegistry toolRegistry;
    private final List<AutoCloseable> registrations = new ArrayList<>();

    SpringPluginContext(String pluginId, ToolRegistry toolRegistry) {
        this.pluginId = Objects.requireNonNull(pluginId, "pluginId");
        this.toolRegistry = Objects.requireNonNull(toolRegistry, "toolRegistry");
    }

    @Override
    public String pluginId() {
        return pluginId;
    }

    @Override
    public AutoCloseable registerTool(ToolDefinition tool) {
        Objects.requireNonNull(tool, "tool");
        PluginToolDefinition adapted = new PluginToolDefinition(
                pluginId,
                new InProcessToolBridge(tool),
                new IPluginToolBridge.PluginToolDescriptor(
                        tool.name(), tool.description(), tool.parameters())
        );
        Runnable disposer = toolRegistry.register(adapted);
        String qualifiedName = adapted.name();
        AutoCloseable registration = new PluginToolRegistration(qualifiedName, disposer::run);
        registrations.add(registration);
        return registration;
    }

    @Override
    public void registerDisposer(AutoCloseable disposer) {
        Objects.requireNonNull(disposer, "disposer");
        registrations.add(disposer);
    }

    List<String> registeredToolNames() {
        return registrations.stream()
                .filter(PluginToolRegistration.class::isInstance)
                .map(PluginToolRegistration.class::cast)
                .map(PluginToolRegistration::qualifiedName)
                .toList();
    }

    @Override
    public synchronized void close() throws Exception {
        List<AutoCloseable> ordered = new ArrayList<>(registrations);
        registrations.clear();
        Exception firstFailure = null;
        for (int index = ordered.size() - 1; index >= 0; index--) {
            try {
                ordered.get(index).close();
            } catch (Exception e) {
                if (firstFailure == null) firstFailure = e;
            }
        }
        if (firstFailure != null) {
            throw firstFailure;
        }
    }

    record PluginToolRegistration(String qualifiedName, AutoCloseable disposer)
            implements AutoCloseable {

        PluginToolRegistration {
            Objects.requireNonNull(qualifiedName, "qualifiedName");
            Objects.requireNonNull(disposer, "disposer");
        }

        @Override
        public void close() throws Exception {
            disposer.close();
        }
    }
}
