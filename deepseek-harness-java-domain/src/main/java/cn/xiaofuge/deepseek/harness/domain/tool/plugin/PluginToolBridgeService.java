package cn.xiaofuge.deepseek.harness.domain.tool.plugin;

import cn.xiaofuge.deepseek.harness.domain.tool.adapter.ToolRegistry;
import cn.xiaofuge.deepseek.harness.domain.agent.service.ScopedSystemPromptAssembler;
import cn.xiaofuge.deepseek.harness.domain.agent.service.SystemPromptAssembler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Domain service that bridges plugin sidecar tools into the {@link ToolRegistry}.
 * <p>
 * When a plugin sidecar starts, this service:
 * <ol>
 *   <li>Connects to the sidecar via {@link IPluginToolBridge}</li>
 *   <li>Lists all tools declared by the sidecar</li>
 *   <li>Wraps each as {@link PluginToolDefinition} and registers in the tool registry</li>
 * </ol>
 * When the sidecar stops, all tools for that plugin are unregistered via disposers.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class PluginToolBridgeService {

    private final ToolRegistry toolRegistry;
    private final IPluginToolBridge pluginToolBridge;
    private final SystemPromptAssembler promptAssembler;
    private final Map<String, List<Runnable>> pluginDisposers = new ConcurrentHashMap<>();
    private final Map<String, List<String>> pluginToolNames = new ConcurrentHashMap<>();

    /**
     * 处理插件 工具 Bridge Service。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param toolRegistry 工具 Registry参数。
     * @param pluginToolBridge 插件 工具 Bridge参数。
     * @return 方法执行结果。
     */
    public PluginToolBridgeService(ToolRegistry toolRegistry, IPluginToolBridge pluginToolBridge) {
        this(toolRegistry, pluginToolBridge, null);
    }

    @Autowired
    public PluginToolBridgeService(
            ToolRegistry toolRegistry,
            IPluginToolBridge pluginToolBridge,
            SystemPromptAssembler promptAssembler
    ) {
        this.toolRegistry = toolRegistry;
        this.pluginToolBridge = pluginToolBridge;
        this.promptAssembler = promptAssembler;
    }

    /**
     * Connect to a plugin sidecar and register all its tools.
     *
     * @param pluginId plugin identifier
     * @param process  the started sidecar process
     * @return list of registered tool names
     */
    public List<String> bridgePlugin(String pluginId, Process process) {
        if (pluginDisposers.containsKey(pluginId)) {
            return List.copyOf(pluginToolNames.getOrDefault(pluginId, List.of()));
        }
        try {
            // Connect
            pluginToolBridge.connect(pluginId, process).join();

            // List tools
            List<IPluginToolBridge.PluginToolDescriptor> descriptors =
                    pluginToolBridge.listTools(pluginId).join();

            // Register each tool
            List<Runnable> disposers = new ArrayList<>();
            List<String> toolNames = new ArrayList<>();

            for (IPluginToolBridge.PluginToolDescriptor desc : descriptors) {
                PluginToolDefinition toolDef = new PluginToolDefinition(pluginId, pluginToolBridge, desc);
                Runnable disposer = toolRegistry.register(toolDef);
                disposers.add(disposer);
                toolNames.add(toolDef.name());
            }

            pluginDisposers.put(pluginId, disposers);
            pluginToolNames.put(pluginId, List.copyOf(toolNames));
            registerPromptSection(pluginId, descriptors);
            return toolNames;

        } catch (Exception e) {
            throw new RuntimeException("Plugin bridge failed: " + e.getMessage(), e);
        }
    }

    /**
     * Disconnect from a plugin sidecar and unregister all its tools.
     *
     * @param pluginId plugin identifier
     */
    public void unbridgePlugin(String pluginId) {
        removePromptSection(pluginId);
        pluginToolNames.remove(pluginId);
        List<Runnable> disposers = pluginDisposers.remove(pluginId);
        if (disposers != null) {
            for (Runnable disposer : disposers) {
                disposer.run();
            }
        }

        try {
            pluginToolBridge.disconnect(pluginId).join();
        } catch (Exception ignored) {
        }
    }

    /**
     * Check if a plugin is currently bridged.
     */
    public boolean isBridged(String pluginId) {
        return pluginDisposers.containsKey(pluginId);
    }

    /**
     * Get the list of tool names registered for a plugin.
     */
    public List<String> getToolNames(String pluginId) {
        return List.copyOf(pluginToolNames.getOrDefault(pluginId, List.of()));
    }

    /**
     * 注册插件贡献的系统提示词片段。
     * <p>流程：校验片段来源 → 登记到提示词装配器 → 供下一轮模型调用使用。</p>
     */
    private void registerPromptSection(
            String pluginId,
            List<IPluginToolBridge.PluginToolDescriptor> descriptors
    ) {
        if (!(promptAssembler instanceof ScopedSystemPromptAssembler scoped) || descriptors.isEmpty()) {
            return;
        }
        StringJoiner lines = new StringJoiner("\n");
        for (var descriptor : descriptors) {
            lines.add("- " + descriptor.name() + ": "
                    + (descriptor.description() == null || descriptor.description().isBlank()
                            ? "no description" : descriptor.description()));
        }
        String text = "## Plugin capabilities: " + pluginId + "\n"
                + "These plugin tools are available in the tool schema. "
                + "Use the matching tool when the request needs its capability.\n"
                + lines;
        scoped.registerSection(new ScopedSystemPromptAssembler.PromptSection(
                promptSectionName(pluginId), 150, text));
    }

    /**
     * 移除 提示词 Section。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param pluginId 插件 ID。
     */
    private void removePromptSection(String pluginId) {
        if (promptAssembler instanceof ScopedSystemPromptAssembler scoped) {
            scoped.removeSection(promptSectionName(pluginId));
        }
    }

    /**
     * 发起提示词 Section 名称。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param pluginId 插件 ID。
     * @return 方法执行结果。
     */
    private String promptSectionName(String pluginId) {
        return "plugin:" + pluginId + ":capabilities";
    }
}
