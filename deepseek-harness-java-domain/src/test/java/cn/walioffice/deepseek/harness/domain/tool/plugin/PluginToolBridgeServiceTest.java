package cn.walioffice.deepseek.harness.domain.tool.plugin;

import cn.walioffice.deepseek.harness.domain.agent.service.ScopedSystemPromptAssembler;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.ContentBlock;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.TextBlock;
import cn.walioffice.deepseek.harness.domain.tool.adapter.ToolRegistry;
import cn.walioffice.deepseek.harness.domain.tool.model.entity.ToolDefinition;
import cn.walioffice.deepseek.harness.domain.tool.model.entity.ToolExecutionResult;
import cn.walioffice.deepseek.harness.domain.tool.model.entity.ToolRunContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginToolBridgeServiceTest {

    @Test
    void bridgesPluginToolsAndRegistersCapabilityPrompt() throws Exception {
        ToolRegistry registry = new TestToolRegistry();
        var assembler = new ScopedSystemPromptAssembler("identity", "", List.of());
        IPluginToolBridge.PluginToolDescriptor descriptor =
                new IPluginToolBridge.PluginToolDescriptor("weather", "Weather lookup", Map.of("type", "object"));
        var bridge = new StubBridge(descriptor);
        var service = new PluginToolBridgeService(registry, bridge, assembler);
        service.bridgePlugin("sample", new ProcessBuilder("true").start());

        List<String> names = service.getToolNames("sample");
        assertEquals(List.of("plugin__sample__weather"), names);
        assertTrue(registry.lookup("plugin__sample__weather").isPresent());
        var prompt = assembler.assemble("agent", null, "/tmp", List.of()).get(1, TimeUnit.SECONDS);
        assertTrue(prompt.system().contains("## Plugin capabilities: sample"));
        assertTrue(prompt.system().contains("Weather lookup"));

        service.unbridgePlugin("sample");
        assertTrue(registry.lookup("plugin__sample__weather").isEmpty());
        assertTrue(service.getToolNames("sample").isEmpty());
    }

    private static final class TestToolRegistry implements ToolRegistry {
        private final Map<String, ToolDefinition> tools = new java.util.concurrent.ConcurrentHashMap<>();

        @Override public Runnable register(ToolDefinition definition) {
            tools.put(definition.name(), definition);
            return () -> tools.remove(definition.name());
        }

        @Override public Optional<ToolDefinition> lookup(String name) {
            return Optional.ofNullable(tools.get(name));
        }

        @Override public java.util.Collection<ToolDefinition> all() {
            return List.copyOf(tools.values());
        }

        @Override public cn.walioffice.deepseek.harness.domain.tool.model.valobj.ToolExecutionMode executionMode(
                cn.walioffice.deepseek.harness.domain.tool.model.entity.ToolExecutionInput exec
        ) {
            return cn.walioffice.deepseek.harness.domain.tool.model.valobj.ToolExecutionMode.EXCLUSIVE;
        }
    }

    private record StubBridge(IPluginToolBridge.PluginToolDescriptor descriptor) implements IPluginToolBridge {
        @Override public CompletableFuture<Void> connect(String pluginId, Process process) {
            return CompletableFuture.completedFuture(null);
        }

        @Override public CompletableFuture<Void> disconnect(String pluginId) {
            return CompletableFuture.completedFuture(null);
        }

        @Override public boolean isConnected(String pluginId) {
            return true;
        }

        @Override public CompletableFuture<List<IPluginToolBridge.PluginToolDescriptor>> listTools(String pluginId) {
            return CompletableFuture.completedFuture(List.of(descriptor));
        }

        @Override public CompletableFuture<IPluginToolBridge.PluginToolResult> callTool(
                String pluginId,
                String toolName,
                Map<String, Object> arguments
        ) {
            return CompletableFuture.completedFuture(new IPluginToolBridge.PluginToolResult(
                    List.<ContentBlock>of(new TextBlock("ok")), false, null
            ));
        }
    }
}
