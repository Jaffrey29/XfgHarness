package cn.xiaofuge.deepseek.harness.domain.plugin.registry.service;

import java.util.Optional;

import cn.xiaofuge.deepseek.harness.domain.plugin.IPluginRegistryService;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.adapter.port.IPluginArtifactInstallerPort;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.adapter.repository.IHarnessPluginRepository;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.entity.HarnessPluginEntity;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.valobj.PluginArtifactInstallPlanVO;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.valobj.PluginInstallModeEnumVO;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.valobj.PluginInstallStatusEnumVO;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.valobj.PluginInstallationResultVO;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.valobj.PluginRuntimeTypeEnumVO;
import java.time.Instant;

/**
 * Owns plugin registration and durable lifecycle status changes.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class PluginRegistryService implements IPluginRegistryService {

    private final IHarnessPluginRepository harnessPluginRepository;
    private final IPluginArtifactInstallerPort pluginArtifactInstallerPort;

    public PluginRegistryService(
            IHarnessPluginRepository harnessPluginRepository,
            IPluginArtifactInstallerPort pluginArtifactInstallerPort
    ) {
        this.harnessPluginRepository = harnessPluginRepository;
        this.pluginArtifactInstallerPort = pluginArtifactInstallerPort;
    }

    @Override
    public PluginInstallationResultVO installPlugin(
            String pluginId,
            String displayName,
            String pluginVersion,
            String runtimeType,
            String sourcePath,
            String entrypoint
    ) {
        requireText(pluginId, "pluginId");
        requireText(displayName, "displayName");
        requireText(pluginVersion, "pluginVersion");
        requireText(runtimeType, "runtimeType");
        requireText(sourcePath, "sourcePath");
        requireText(entrypoint, "entrypoint");

        PluginRuntimeTypeEnumVO resolvedRuntimeType = PluginRuntimeTypeEnumVO.valueOf(runtimeType);
        PluginInstallModeEnumVO installMode = resolvedRuntimeType == PluginRuntimeTypeEnumVO.JAVA_NATIVE
                ? PluginInstallModeEnumVO.NATIVE
                : PluginInstallModeEnumVO.NODE_BRIDGE;
        Instant installedAt = Instant.now();
        PluginArtifactInstallPlanVO installPlan = pluginArtifactInstallerPort.install(pluginId, sourcePath, entrypoint);

        HarnessPluginEntity plugin = new HarnessPluginEntity(
                pluginId,
                displayName,
                pluginVersion,
                resolvedRuntimeType,
                installMode,
                installPlan.installedSourcePath(),
                installPlan.normalizedEntrypoint(),
                PluginInstallStatusEnumVO.REGISTERED,
                installedAt
        );
        harnessPluginRepository.save(plugin);

        return new PluginInstallationResultVO(
                plugin.getPluginId(),
                plugin.getRuntimeType(),
                plugin.getInstallMode(),
                plugin.getStatus(),
                plugin.getInstallMode() == PluginInstallModeEnumVO.NATIVE,
                plugin.getInstalledAt()
        );
    }

    @Override
    public HarnessPluginEntity loadPlugin(String pluginId) {
        requireText(pluginId, "pluginId");
        return harnessPluginRepository.findByPluginId(pluginId)
                .orElseThrow(() -> new IllegalArgumentException("plugin not found: " + pluginId));
    }

    @Override
    public Optional<HarnessPluginEntity> loadPluginOptional(String pluginId) {
        requireText(pluginId, "pluginId");
        return harnessPluginRepository.findByPluginId(pluginId);
    }

    @Override
    public PluginInstallStatusEnumVO refreshStatus(HarnessPluginEntity plugin, boolean bridgeReady) {
        if (bridgeReady) {
            plugin.activate();
        } else {
            plugin.fail();
        }
        harnessPluginRepository.save(plugin);
        return plugin.getStatus();
    }

    @Override
    public PluginInstallStatusEnumVO disablePlugin(String pluginId) {
        HarnessPluginEntity plugin = loadPlugin(pluginId);
        plugin.disable();
        harnessPluginRepository.save(plugin);
        return plugin.getStatus();
    }

    @Override
    public PluginInstallStatusEnumVO enablePlugin(String pluginId) {
        HarnessPluginEntity plugin = loadPlugin(pluginId);
        plugin.enable();
        harnessPluginRepository.save(plugin);
        return plugin.getStatus();
    }

    @Override
    public PluginInstallStatusEnumVO uninstallPlugin(String pluginId) {
        HarnessPluginEntity plugin = loadPlugin(pluginId);
        plugin.uninstall();
        harnessPluginRepository.save(plugin);
        pluginArtifactInstallerPort.uninstall(pluginId);
        return plugin.getStatus();
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
