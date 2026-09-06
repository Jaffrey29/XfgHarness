package cn.walioffice.deepseek.harness.infrastructure.adapter.plugin.java;

import cn.walioffice.deepseek.harness.domain.plugin.spi.JavaHarnessPlugin;
import cn.walioffice.deepseek.harness.domain.plugin.spi.PluginManifest;
import cn.walioffice.deepseek.harness.domain.tool.model.entity.ToolDefinition;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

/**
 * Loads one Java-native plugin JAR with an isolated {@link URLClassLoader},
 * discovers the {@link JavaHarnessPlugin} implementation via ServiceLoader
 * (or the manifest entrypoint), and bridges its tools into the harness.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Component
public class JavaPluginLoader {

    private static final Logger log = LoggerFactory.getLogger(JavaPluginLoader.class);

    /**
     * Load result: the loaded plugin instance, its manifest, and the
     * ClassLoader that owns the JAR (must be closed on unload).
     */
    public record LoadedPlugin(
            JavaHarnessPlugin instance,
            PluginManifest manifest,
            URLClassLoader classLoader,
            List<ToolDefinition> tools
    ) {}

    /**
     * Load a plugin JAR from the given path.
     *
     * @param jarPath absolute path to the plugin JAR
     * @return loaded plugin
     * @throws IOException if the JAR cannot be read
     */
    public LoadedPlugin load(Path jarPath) throws IOException {
        if (!Files.exists(jarPath) || !Files.isRegularFile(jarPath)) {
            throw new IllegalArgumentException("plugin JAR not found: " + jarPath);
        }

        URLClassLoader classLoader = new URLClassLoader(
                new URL[]{jarPath.toUri().toURL()},
                Thread.currentThread().getContextClassLoader()
        );

        PluginManifest manifest = readManifest(classLoader).orElseThrow(() -> {
            closeQuietly(classLoader);
            return new IllegalStateException("META-INF/plugin.yaml not found in plugin JAR: " + jarPath);
        });

        JavaHarnessPlugin plugin = instantiate(manifest, classLoader);
        plugin.onStart();
        List<ToolDefinition> tools = plugin.tools();
        if (tools == null) tools = List.of();

        log.info("Java plugin loaded: {} v{} ({} tools)", manifest.id(), manifest.version(), tools.size());
        return new LoadedPlugin(plugin, manifest, classLoader, tools);
    }

    /**
     * Unload a previously loaded plugin: call onStop, close the ClassLoader.
     */
    public void unload(LoadedPlugin loaded) {
        try {
            loaded.instance().onStop();
        } catch (Exception e) {
            log.warn("Plugin onStop() failed for {}: {}", loaded.manifest().id(), e.getMessage());
        }
        closeQuietly(loaded.classLoader());
    }

    private Optional<PluginManifest> readManifest(URLClassLoader classLoader) {
        URL url = classLoader.findResource("META-INF/plugin.yaml");
        if (url == null) return Optional.empty();
        try (InputStream in = url.openStream()) {
            Yaml yaml = new Yaml();
            java.util.Map<String, Object> map = yaml.load(in);
            if (map == null) return Optional.empty();
            String id = str(map.get("id"));
            String name = str(map.get("name"));
            String version = str(map.get("version"));
            String author = str(map.get("author"));
            String description = str(map.get("description"));
            String entrypoint = str(map.get("entrypoint"));
            if (id == null || name == null || version == null) return Optional.empty();
            return Optional.of(new PluginManifest(id, name, version, author, description, entrypoint));
        } catch (IOException e) {
            log.warn("Failed to read plugin.yaml: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private JavaHarnessPlugin instantiate(PluginManifest manifest, URLClassLoader classLoader) {
        String entrypoint = manifest.entrypoint();

        if (entrypoint != null && !entrypoint.isBlank()) {
            try {
                Class<?> clazz = classLoader.loadClass(entrypoint);
                return (JavaHarnessPlugin) clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new IllegalStateException(
                        "failed to instantiate plugin entrypoint " + entrypoint + ": " + e.getMessage(), e);
            }
        }

        ServiceLoader<JavaHarnessPlugin> serviceLoader =
                ServiceLoader.load(JavaHarnessPlugin.class, classLoader);
        List<JavaHarnessPlugin> found = new ArrayList<>();
        for (JavaHarnessPlugin candidate : serviceLoader) {
            if (manifest.id().equals(candidate.pluginId())) {
                found.add(candidate);
            }
        }
        if (found.isEmpty()) {
            throw new IllegalStateException(
                    "no ServiceLoader entry found for plugin '" + manifest.id()
                            + "'. Ensure META-INF/services/"
                            + JavaHarnessPlugin.class.getName()
                            + " exists in the JAR, or set 'entrypoint' in plugin.yaml.");
        }
        if (found.size() > 1) {
            throw new IllegalStateException(
                    "multiple ServiceLoader entries found for plugin '" + manifest.id() + "'");
        }
        return found.get(0);
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static void closeQuietly(URLClassLoader cl) {
        try {
            cl.close();
        } catch (IOException ignored) {
        }
    }
}
