package cn.xiaofuge.deepseek.harness.infrastructure.adapter.plugin.java;

import cn.xiaofuge.deepseek.harness.domain.spi.PluginManifest;
import cn.xiaofuge.deepseek.harness.domain.agent.service.ScopedSystemPromptAssembler;
import cn.xiaofuge.deepseek.harness.domain.agent.service.SystemPromptAssembler;
import cn.xiaofuge.deepseek.harness.domain.tool.adapter.ToolRegistry;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolDefinition;
import cn.xiaofuge.deepseek.harness.domain.tool.plugin.IPluginToolBridge;
import cn.xiaofuge.deepseek.harness.domain.tool.plugin.PluginToolDefinition;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 进程内 Java 原生插件生命周期管理器。
 * <p>与 Node sidecar 不同，Java 插件不会拉起子进程，而是在宿主 JVM 中通过隔离
 * URLClassLoader 加载。核心流程为：加载 JAR -> onStart -> 工具注册 ->
 * 提示词登记 -> 停止时注销工具并关闭 ClassLoader。
 * <p>数据示例：插件 id="time"，工具 name="now"，注册后的 Agent 工具名为
 * `plugin__time__now`。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Component
public class JavaPluginRuntimeManager {

    private static final Logger log = LoggerFactory.getLogger(JavaPluginRuntimeManager.class);

    private final JavaPluginLoader loader;
    private final ToolRegistry toolRegistry;
    private final SystemPromptAssembler promptAssembler;
    private final Path installRoot;

    private final Map<String, RunningJavaPlugin> runningPlugins = new ConcurrentHashMap<>();

    /**
     * 注入类加载器、工具注册表、提示词装配器和插件安装根目录。
     */
    public JavaPluginRuntimeManager(
            JavaPluginLoader loader,
            ToolRegistry toolRegistry,
            SystemPromptAssembler promptAssembler,
            @Value("${harness.plugin.install-root:./plugins}") String installRoot
    ) {
        this.loader = loader;
        this.toolRegistry = toolRegistry;
        this.promptAssembler = promptAssembler;
        this.installRoot = Path.of(installRoot).toAbsolutePath().normalize();
    }

    /**
     * 启动一个 Java 原生插件。
     * <p>执行流程：重复启动直接返回 -> loader.load 创建隔离 ClassLoader 并执行 onStart ->
     * 逐个包装为 PluginToolDefinition -> 注册 ToolRegistry -> 保存运行状态 -> 写入提示词能力段。
     * <p>数据示例：jarPath=/plugins/time/time.jar，包含 now 工具，注册后工具名为
     * plugin__time__now。
     *
     * @param pluginId 插件登记 ID
     * @param jarPath  已安装插件 JAR 的路径
     * @return 启动结果
     */
    public JavaPluginStartResult start(String pluginId, Path jarPath) {
        RunningJavaPlugin existing = runningPlugins.get(pluginId);
        if (existing != null) {
            return new JavaPluginStartResult(pluginId, true, "java plugin already running",
                    existing.manifest.name(), existing.manifest.version(), existing.startedAt);
        }

        try {
            JavaPluginLoader.LoadedPlugin loaded = loader.load(jarPath);
            List<Runnable> disposers = new ArrayList<>();
            List<String> toolNames = new ArrayList<>();

            for (ToolDefinition tool : loaded.tools()) {
                PluginToolDefinition adapted = new PluginToolDefinition(
                        pluginId, new InProcessToolBridge(tool), toDescriptor(tool));
                Runnable disposer = toolRegistry.register(adapted);
                disposers.add(disposer);
                String qualifiedName = "plugin__" + pluginId + "__" + tool.name();
                toolNames.add(qualifiedName);
            }

            Instant startedAt = Instant.now();
            runningPlugins.put(pluginId, new RunningJavaPlugin(
                    loaded, loaded.manifest(), disposers, startedAt, toolNames
            ));
            registerPromptSection(pluginId, toolNames, loaded.manifest().description());
            return new JavaPluginStartResult(pluginId, true, "java plugin started",
                    loaded.manifest().name(), loaded.manifest().version(), startedAt);
        } catch (Exception e) {
            log.error("Failed to start Java plugin {}: {}", pluginId, e.getMessage(), e);
            return new JavaPluginStartResult(pluginId, false,
                    "failed to start: " + e.getMessage(), null, null, null);
        }
    }

    /**
     * 停止一个运行中的 Java 插件。
     * <p>执行顺序：从运行表移除 -> 逆登记工具 -> 移除提示词能力段 -> 执行插件 onStop -> 关闭 ClassLoader。
     * 单个工具注销失败不阻塞其他清理动作，但会记录警告。
     */
    public JavaPluginStatusResult stop(String pluginId) {
        RunningJavaPlugin plugin = runningPlugins.remove(pluginId);
        if (plugin == null) {
            return new JavaPluginStatusResult(pluginId, false, "java plugin not running");
        }
        for (Runnable disposer : plugin.disposers) {
            try {
                disposer.run();
            } catch (Exception e) {
                log.warn("Disposer failed for plugin {}: {}", pluginId, e.getMessage());
            }
        }
        removePromptSection(pluginId);
        loader.unload(plugin.loaded);
        return new JavaPluginStatusResult(pluginId, false, "java plugin stopped");
    }

    /**
     * 查询 Java 插件的进程内运行状态。
     */
    public JavaPluginStatusResult status(String pluginId) {
        RunningJavaPlugin plugin = runningPlugins.get(pluginId);
        if (plugin == null) {
            return new JavaPluginStatusResult(pluginId, false, "java plugin not running");
        }
        return new JavaPluginStatusResult(pluginId, true, "java plugin is running");
    }

    /**
     * 解析已安装插件对应的 JAR 文件。
     * <p>优先使用 sourcePath 本身；其次尝试 entrypoint 指向的 JAR；
     * 最后尝试 `<目录名>.jar` 约定路径，或在目录中找到 plugin.yaml 时返回该目录。
     */
    public Optional<Path> resolveJarPath(String sourcePath, String entrypoint) {
        Path source = Path.of(sourcePath).toAbsolutePath().normalize();
        if (Files.isRegularFile(source) && source.toString().endsWith(".jar")) {
            return Optional.of(source);
        }
        if (entrypoint != null && entrypoint.endsWith(".jar")) {
            Path jar = source.resolve(entrypoint).normalize();
            if (Files.isRegularFile(jar)) return Optional.of(jar);
        }
        Path defaultJar = source.resolve(source.getFileName().toString() + ".jar");
        if (Files.isRegularFile(defaultJar)) return Optional.of(defaultJar);
        Path pluginYaml = source.resolve("META-INF/plugin.yaml");
        if (Files.exists(pluginYaml)) return Optional.of(source);
        return Optional.empty();
    }

    /**
     * 把插件工具定义转换为工具桥接描述符。
     */
    private IPluginToolBridge.PluginToolDescriptor toDescriptor(ToolDefinition tool) {
        return new IPluginToolBridge.PluginToolDescriptor(
                tool.name(), tool.description(), tool.parameters());
    }

    /**
     * 在系统提示词中注册插件能力说明。
     * <p>提示词列出插件描述和完整工具名，引导模型按需调用对应插件工具。
     */
    private void registerPromptSection(
            String pluginId,
            List<String> toolNames,
            String description
    ) {
        if (!(promptAssembler instanceof ScopedSystemPromptAssembler scoped) || toolNames.isEmpty()) {
            return;
        }
        String capability = description == null || description.isBlank()
                ? "No plugin description provided." : description;
        String text = "## Plugin capabilities: " + pluginId + "\n"
                + capability + "\nAvailable tools: " + String.join(", ", toolNames) + "\n"
                + "Use the matching tool when the request needs this capability.";
        scoped.registerSection(new ScopedSystemPromptAssembler.PromptSection(
                promptSectionName(pluginId), 150, text));
    }

    /**
     * 从系统提示词中移除插件能力说明。
     */
    private void removePromptSection(String pluginId) {
        if (promptAssembler instanceof ScopedSystemPromptAssembler scoped) {
            scoped.removeSection(promptSectionName(pluginId));
        }
    }

    /**
     * 生成插件提示词段的稳定名称。
     */
    private String promptSectionName(String pluginId) {
        return "plugin:" + pluginId + ":capabilities";
    }

    /**
     * 一个正在运行的 Java 插件的加载结果、工具注销器和状态快照。
     */
    private record RunningJavaPlugin(
            JavaPluginLoader.LoadedPlugin loaded,
            PluginManifest manifest,
            List<Runnable> disposers,
            Instant startedAt,
            List<String> toolNames
    ) {}

    /**
     * Java 插件启动结果。
     */
    public record JavaPluginStartResult(
            String pluginId, boolean running, String message,
            String displayName, String version, Instant startedAt
    ) {}

    /**
     * Java 插件运行状态结果。
     */
    public record JavaPluginStatusResult(
            String pluginId, boolean running, String message
    ) {}
}
