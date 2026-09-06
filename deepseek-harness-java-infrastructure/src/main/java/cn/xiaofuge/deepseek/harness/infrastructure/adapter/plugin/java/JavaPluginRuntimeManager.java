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
 * Manages the lifecycle of in-process Java-native plugins.
 * <p>
 * Unlike the Node sidecar model, Java plugins run inside the harness JVM
 * itself — no subprocess is spawned. This manager:
 * <ul>
 *   <li>Loads plugin JARs with isolated ClassLoaders</li>
 *   <li>Registers each plugin's tools into the {@link ToolRegistry}</li>
 *   <li>Tracks running state per plugin</li>
 *   <li>Unregisters tools and unloads ClassLoaders on stop</li>
 * </ul>
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
     * Start one Java plugin: load the JAR, register tools.
     *
     * @param pluginId       plugin identifier
     * @param jarPath        absolute path to the installed plugin JAR
     * @return running result
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
     * Stop one running Java plugin: unregister tools, close ClassLoader.
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
     * Query running status.
     */
    public JavaPluginStatusResult status(String pluginId) {
        RunningJavaPlugin plugin = runningPlugins.get(pluginId);
        if (plugin == null) {
            return new JavaPluginStatusResult(pluginId, false, "java plugin not running");
        }
        return new JavaPluginStatusResult(pluginId, true, "java plugin is running");
    }

    /**
     * Resolve the JAR path for one installed plugin.
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

    private IPluginToolBridge.PluginToolDescriptor toDescriptor(ToolDefinition tool) {
        return new IPluginToolBridge.PluginToolDescriptor(
                tool.name(), tool.description(), tool.parameters());
    }

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

    private void removePromptSection(String pluginId) {
        if (promptAssembler instanceof ScopedSystemPromptAssembler scoped) {
            scoped.removeSection(promptSectionName(pluginId));
        }
    }

    private String promptSectionName(String pluginId) {
        return "plugin:" + pluginId + ":capabilities";
    }

    private record RunningJavaPlugin(
            JavaPluginLoader.LoadedPlugin loaded,
            PluginManifest manifest,
            List<Runnable> disposers,
            Instant startedAt,
            List<String> toolNames
    ) {}

    public record JavaPluginStartResult(
            String pluginId, boolean running, String message,
            String displayName, String version, Instant startedAt
    ) {}

    public record JavaPluginStatusResult(
            String pluginId, boolean running, String message
    ) {}
}
