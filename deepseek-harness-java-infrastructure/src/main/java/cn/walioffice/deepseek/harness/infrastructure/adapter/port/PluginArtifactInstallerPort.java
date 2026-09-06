package cn.walioffice.deepseek.harness.infrastructure.adapter.port;

import cn.walioffice.deepseek.harness.domain.plugin.registry.adapter.port.IPluginArtifactInstallerPort;
import cn.walioffice.deepseek.harness.domain.plugin.registry.model.valobj.PluginArtifactInstallPlanVO;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Copies plugin source trees (Node plugins) or JAR files (Java plugins) into
 * the managed install root.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Component
public class PluginArtifactInstallerPort implements IPluginArtifactInstallerPort {

    private final Path installRoot;

    public PluginArtifactInstallerPort(@Value("${harness.plugin.install-root:./plugins}") String installRoot) {
        this.installRoot = Path.of(installRoot).toAbsolutePath().normalize();
    }

    @Override
    public PluginArtifactInstallPlanVO install(String pluginId, String sourcePath, String entrypoint) {
        Path source = Path.of(sourcePath).toAbsolutePath().normalize();
        if (!Files.exists(source)) {
            throw new IllegalArgumentException("plugin source path does not exist: " + sourcePath);
        }

        if (Files.isRegularFile(source) && source.toString().endsWith(".jar")) {
            return installJar(pluginId, source);
        }
        if (!Files.isDirectory(source)) {
            throw new IllegalArgumentException("plugin source path must be a directory or a .jar file");
        }

        return installDirectory(pluginId, source, entrypoint);
    }

    @Override
    public void uninstall(String pluginId) {
        Path targetRoot = installRoot.resolve(pluginId).normalize();
        if (!targetRoot.startsWith(installRoot)) {
            throw new IllegalStateException("plugin uninstall path escapes install root");
        }
        try {
            deleteDirectoryIfExists(targetRoot);
            Path jarFile = installRoot.resolve(pluginId + ".jar");
            Files.deleteIfExists(jarFile);
        } catch (IOException exception) {
            throw new IllegalStateException("failed to remove plugin artifacts", exception);
        }
    }

    private PluginArtifactInstallPlanVO installJar(String pluginId, Path jarSource) {
        try {
            Files.createDirectories(installRoot);
            Path targetJar = installRoot.resolve(pluginId + ".jar").normalize();
            if (!targetJar.startsWith(installRoot)) {
                throw new IllegalStateException("plugin install path escapes install root");
            }
            Files.copy(jarSource, targetJar, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            return new PluginArtifactInstallPlanVO(targetJar.toString(), targetJar.getFileName().toString());
        } catch (IOException exception) {
            throw new IllegalStateException("failed to install plugin JAR", exception);
        }
    }

    private PluginArtifactInstallPlanVO installDirectory(String pluginId, Path sourceRoot, String entrypoint) {
        Path normalizedEntrypoint = normalizeEntrypoint(sourceRoot, entrypoint);
        Path targetRoot = installRoot.resolve(pluginId).normalize();
        if (!targetRoot.startsWith(installRoot)) {
            throw new IllegalStateException("plugin install path escapes install root");
        }

        try {
            Files.createDirectories(installRoot);
            replaceDirectory(sourceRoot, targetRoot);
        } catch (IOException exception) {
            throw new IllegalStateException("failed to install plugin artifacts", exception);
        }

        String relativeEntrypoint = sourceRoot.relativize(normalizedEntrypoint).toString();
        return new PluginArtifactInstallPlanVO(targetRoot.toString(), relativeEntrypoint);
    }

    private Path normalizeEntrypoint(Path sourceRoot, String entrypoint) {
        Path entrypointPath = Path.of(entrypoint);
        Path normalized = entrypointPath.isAbsolute()
                ? entrypointPath.toAbsolutePath().normalize()
                : sourceRoot.resolve(entrypointPath).normalize();
        if (!normalized.startsWith(sourceRoot)) {
            throw new IllegalArgumentException("plugin entrypoint must stay inside source path");
        }
        if (!Files.exists(normalized) || Files.isDirectory(normalized)) {
            throw new IllegalArgumentException("plugin entrypoint must be an existing file");
        }
        return normalized;
    }

    private void replaceDirectory(Path sourceRoot, Path targetRoot) throws IOException {
        deleteDirectoryIfExists(targetRoot);
        Files.walkFileTree(sourceRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path relative = sourceRoot.relativize(dir);
                Files.createDirectories(targetRoot.resolve(relative));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path relative = sourceRoot.relativize(file);
                Files.copy(file, targetRoot.resolve(relative), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void deleteDirectoryIfExists(Path targetRoot) throws IOException {
        if (!Files.exists(targetRoot)) {
            return;
        }
        Files.walkFileTree(targetRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exception) throws IOException {
                if (exception != null) {
                    throw exception;
                }
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
