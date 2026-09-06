package cn.walioffice.deepseek.harness.infrastructure.adapter.port;

import cn.walioffice.deepseek.harness.domain.plugin.bridge.adapter.port.IPluginRuntimeBridgePort;
import cn.walioffice.deepseek.harness.domain.plugin.bridge.model.valobj.PluginBridgePlanVO;
import cn.walioffice.deepseek.harness.domain.plugin.bridge.model.valobj.PluginPackageTypeEnumVO;
import cn.walioffice.deepseek.harness.domain.plugin.registry.model.entity.HarnessPluginEntity;
import cn.walioffice.deepseek.harness.domain.plugin.registry.model.valobj.PluginRuntimeTypeEnumVO;
import cn.walioffice.deepseek.harness.infrastructure.adapter.plugin.java.JavaPluginRuntimeManager;
import cn.walioffice.deepseek.harness.infrastructure.gateway.PluginManifestGatewayService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Resolves execution plans for both Java-native and Node-bridge plugins.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Component
public class PluginRuntimeBridgePort implements IPluginRuntimeBridgePort {

    private final PluginManifestGatewayService pluginManifestGatewayService;
    private final JavaPluginRuntimeManager javaPluginRuntimeManager;

    public PluginRuntimeBridgePort(
            PluginManifestGatewayService pluginManifestGatewayService,
            JavaPluginRuntimeManager javaPluginRuntimeManager
    ) {
        this.pluginManifestGatewayService = pluginManifestGatewayService;
        this.javaPluginRuntimeManager = javaPluginRuntimeManager;
    }

    @Override
    public PluginBridgePlanVO inspect(HarnessPluginEntity plugin) {
        Path sourcePath = Path.of(plugin.getSourcePath()).toAbsolutePath().normalize();
        if (!Files.exists(sourcePath)) {
            return new PluginBridgePlanVO(
                    PluginPackageTypeEnumVO.UNKNOWN,
                    null,
                    sourcePath.toString(),
                    null,
                    null,
                    false,
                    "plugin source path does not exist"
            );
        }

        if (plugin.getRuntimeType() == PluginRuntimeTypeEnumVO.JAVA_NATIVE) {
            return inspectJavaNative(plugin, sourcePath);
        }
        return inspectNodeBridge(plugin, sourcePath);
    }

    private PluginBridgePlanVO inspectJavaNative(HarnessPluginEntity plugin, Path sourcePath) {
        Optional<Path> jarPath = javaPluginRuntimeManager.resolveJarPath(
                sourcePath.toString(), plugin.getEntrypoint());

        if (jarPath.isEmpty()) {
            return new PluginBridgePlanVO(
                    PluginPackageTypeEnumVO.UNKNOWN,
                    null,
                    sourcePath.toString(),
                    null,
                    null,
                    false,
                    "java-native plugin JAR not found under source path"
            );
        }

        Path jar = jarPath.get();
        String manifestPath = jar.toString();
        String runtimeCommand = "java-in-process:" + plugin.getPluginId();

        return new PluginBridgePlanVO(
                PluginPackageTypeEnumVO.DSH_PACKAGE,
                manifestPath,
                jar.getParent() != null ? jar.getParent().toString() : sourcePath.toString(),
                jar.toString(),
                runtimeCommand,
                true,
                "java-native plugin JAR ready for in-process loading"
        );
    }

    private PluginBridgePlanVO inspectNodeBridge(HarnessPluginEntity plugin, Path sourcePath) {
        Path codexManifestPath = sourcePath.resolve(".codex-plugin/plugin.json");
        Path packageJsonPath = sourcePath.resolve("package.json");
        Path cordisPath = sourcePath.resolve("cordis.yml");

        if (Files.exists(codexManifestPath)) {
            return inspectCodexPlugin(plugin, sourcePath, codexManifestPath);
        }
        if (Files.exists(packageJsonPath)) {
            return inspectDshPackage(plugin, sourcePath, packageJsonPath, cordisPath);
        }
        if (Files.exists(cordisPath)) {
            return new PluginBridgePlanVO(
                    PluginPackageTypeEnumVO.CORDIS_PROFILE,
                    cordisPath.toString(),
                    sourcePath.toString(),
                    null,
                    "pnpm dsh --profile headless --patch " + cordisPath,
                    false,
                    "cordis profile detected, but direct activation still needs a dedicated dsh sidecar session runner"
            );
        }

        return new PluginBridgePlanVO(
                PluginPackageTypeEnumVO.UNKNOWN,
                null,
                sourcePath.toString(),
                null,
                null,
                false,
                "no supported plugin manifest found under source path"
        );
    }

    private PluginBridgePlanVO inspectCodexPlugin(HarnessPluginEntity plugin, Path sourcePath, Path codexManifestPath) {
        Optional<Map<String, Object>> manifest = pluginManifestGatewayService.readJson(codexManifestPath);
        String message = manifest
                .map(map -> "codex plugin '" + map.getOrDefault("name", plugin.getPluginId()) + "' detected; metadata is readable but execution still depends on Codex host semantics")
                .orElse("codex plugin manifest detected");
        return new PluginBridgePlanVO(
                PluginPackageTypeEnumVO.CODEX_PLUGIN,
                codexManifestPath.toString(),
                sourcePath.toString(),
                null,
                null,
                false,
                message
        );
    }

    private PluginBridgePlanVO inspectDshPackage(HarnessPluginEntity plugin, Path sourcePath, Path packageJsonPath, Path cordisPath) {
        Map<String, Object> manifest = pluginManifestGatewayService.readJson(packageJsonPath)
                .orElseThrow(() -> new IllegalStateException("package.json exists but could not be parsed"));
        String entrypoint = resolveEntrypoint(plugin, sourcePath, manifest);
        boolean hasEntrypoint = entrypoint != null;
        String runtimeCommand = hasEntrypoint ? "node " + entrypoint : null;
        String message;
        if (hasEntrypoint) {
            message = "dsh package detected with executable Node entrypoint";
        } else if (Files.exists(cordisPath)) {
            message = "dsh package detected, but only cordis composition is present; bridge execution needs a dsh sidecar launcher";
        } else {
            message = "dsh package detected, but no executable entrypoint was resolved";
        }
        return new PluginBridgePlanVO(
                PluginPackageTypeEnumVO.DSH_PACKAGE,
                packageJsonPath.toString(),
                sourcePath.toString(),
                entrypoint,
                runtimeCommand,
                hasEntrypoint,
                message
        );
    }

    private String resolveEntrypoint(HarnessPluginEntity plugin, Path sourcePath, Map<String, Object> manifest) {
        String requestedEntrypoint = plugin.getEntrypoint();
        if (requestedEntrypoint != null && !requestedEntrypoint.isBlank()) {
            Path requestedPath = sourcePath.resolve(requestedEntrypoint).normalize();
            if (Files.exists(requestedPath)) {
                return requestedPath.toString();
            }
        }
        Object main = manifest.get("main");
        if (main instanceof String mainValue && !mainValue.isBlank()) {
            Path mainPath = sourcePath.resolve(mainValue).normalize();
            if (Files.exists(mainPath)) {
                return mainPath.toString();
            }
        }
        return null;
    }
}
