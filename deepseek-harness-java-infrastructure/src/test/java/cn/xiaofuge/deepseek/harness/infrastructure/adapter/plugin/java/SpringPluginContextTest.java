package cn.xiaofuge.deepseek.harness.infrastructure.adapter.plugin.java;

import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolDefinition;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolExecutionResult;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolRunContext;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.TextBlock;
import cn.xiaofuge.deepseek.harness.domain.spi.JavaHarnessPlugin;
import cn.xiaofuge.deepseek.harness.domain.spi.PluginContext;
import cn.xiaofuge.deepseek.harness.domain.tool.adapter.ToolRegistry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class SpringPluginContextTest {

    @Test
    void registersLegacyToolsWithQualifiedNames() {
        RecordingToolRegistry registry = new RecordingToolRegistry();
        SpringPluginContext context = new SpringPluginContext("demo", registry);
        JavaHarnessPlugin plugin = new JavaHarnessPlugin() {
            @Override public String pluginId() { return "demo"; }
            @Override public List<ToolDefinition> tools() { return List.of(tool("echo")); }
        };

        plugin.configure(context);

        assertEquals(List.of("plugin__demo__echo"), context.registeredToolNames());
        assertEquals(List.of("plugin__demo__echo"), registry.registeredNames);
    }

    @Test
    void closesRegistrationsInReverseOrder() throws Exception {
        RecordingToolRegistry registry = new RecordingToolRegistry();
        SpringPluginContext context = new SpringPluginContext("demo", registry);
        List<String> disposed = new java.util.ArrayList<>();
        context.registerTool(tool("one"));
        context.registerTool(tool("two"));
        context.registerDisposer(() -> disposed.add("resource"));

        context.close();

        assertEquals(List.of("resource"), disposed);
        assertEquals(List.of("plugin__demo__two", "plugin__demo__one"), registry.unregisteredNames);
        assertTrue(context.registeredToolNames().isEmpty());
    }

    private ToolDefinition tool(String name) {
        return new ToolDefinition() {
            @Override public String name() { return name; }
            @Override public String description() { return "test tool"; }
            @Override public Map<String, Object> parameters() { return Map.of("type", "object"); }
            @Override public CompletableFuture<ToolExecutionResult> execute(Object args, ToolRunContext ctx) {
                return CompletableFuture.completedFuture(
                        (ToolExecutionResult) ToolExecutionResult.ok(List.of(new TextBlock("ok"))));
            }
        };
    }

    private static final class RecordingToolRegistry implements ToolRegistry {
        private final Map<String, ToolDefinition> tools = new LinkedHashMap<>();
        private final List<String> registeredNames = new java.util.ArrayList<>();
        private final List<String> unregisteredNames = new java.util.ArrayList<>();

        @Override public Runnable register(ToolDefinition definition) {
            registeredNames.add(definition.name());
            tools.put(definition.name(), definition);
            return () -> {
                tools.remove(definition.name());
                unregisteredNames.add(definition.name());
            };
        }

        @Override public Optional<ToolDefinition> lookup(String name) {
            return Optional.ofNullable(tools.get(name));
        }

        @Override public java.util.Collection<ToolDefinition> all() {
            return List.copyOf(tools.values());
        }

        @Override public cn.xiaofuge.deepseek.harness.domain.model.valobj.ToolExecutionMode executionMode(
                cn.xiaofuge.deepseek.harness.domain.model.entity.ToolExecutionInput input) {
            return cn.xiaofuge.deepseek.harness.domain.model.valobj.ToolExecutionMode.EXCLUSIVE;
        }
    }
}
