package cn.xiaofuge.deepseek.harness.app.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cn.xiaofuge.deepseek.harness.api.dto.InstallPluginRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.RunPluginRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.RunPluginResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.plugin.IInstallHarnessPluginCase;
import cn.xiaofuge.deepseek.harness.cases.plugin.IRunHarnessPluginCase;
import cn.xiaofuge.deepseek.harness.domain.plugin.IPluginRegistryService;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.adapter.repository.IHarnessPluginRepository;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.entity.HarnessPluginEntity;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.valobj.PluginInstallModeEnumVO;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.valobj.PluginInstallStatusEnumVO;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.valobj.PluginRuntimeTypeEnumVO;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.valobj.PluginInstallationResultVO;
import cn.xiaofuge.deepseek.harness.infrastructure.config.HarnessExtensionsProperties;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PresetPluginLoaderTest {

    @Test
    void restoresPersistedActivePluginAfterHostRestart() {
        HarnessPluginEntity activePlugin = new HarnessPluginEntity(
                "mall-weekend-assistant",
                "2D Weekend Mall Assistant",
                "1.0.0-SNAPSHOT",
                PluginRuntimeTypeEnumVO.JAVA_NATIVE,
                PluginInstallModeEnumVO.NATIVE,
                "/plugins/mall-weekend-assistant.jar",
                "mall-weekend-assistant.jar",
                PluginInstallStatusEnumVO.ACTIVE,
                Instant.now()
        );
        List<HarnessPluginEntity> plugins = List.of(activePlugin);
        List<String> runPluginIds = new ArrayList<>();
        Fixture fixture = new Fixture(plugins, runPluginIds);
        PresetPluginLoader loader = new PresetPluginLoader(
                fixture.properties,
                fixture.installCase,
                fixture.runCase,
                fixture.registryService,
                fixture.repository
        );

        loader.run(null);

        assertEquals(List.of("mall-weekend-assistant"), runPluginIds);
    }

    @Test
    void doesNotRestoreActivePresetTwice() {
        HarnessPluginEntity activePreset = new HarnessPluginEntity(
                "sample-tools",
                "Sample Tools Plugin",
                "1.0.0",
                PluginRuntimeTypeEnumVO.JAVA_NATIVE,
                PluginInstallModeEnumVO.NATIVE,
                "/plugins/sample-tools.jar",
                "sample-tools.jar",
                PluginInstallStatusEnumVO.ACTIVE,
                Instant.now()
        );
        List<String> runPluginIds = new ArrayList<>();
        Fixture fixture = new Fixture(List.of(activePreset), runPluginIds);
        fixture.properties.getPlugins().setPreset(List.of(preset("sample-tools")));
        PresetPluginLoader loader = new PresetPluginLoader(
                fixture.properties,
                fixture.installCase,
                fixture.runCase,
                fixture.registryService,
                fixture.repository
        );

        loader.run(null);

        assertEquals(List.of("sample-tools"), runPluginIds);
    }

    private HarnessExtensionsProperties.PresetPlugin preset(String pluginId) {
        HarnessExtensionsProperties.PresetPlugin preset = new HarnessExtensionsProperties.PresetPlugin();
        preset.setPluginId(pluginId);
        preset.setDisplayName(pluginId);
        preset.setPluginVersion("1.0.0");
        preset.setRuntimeType("JAVA_NATIVE");
        preset.setSourcePath("./plugins/" + pluginId + ".jar");
        preset.setEntrypoint(pluginId + ".jar");
        preset.setAutoStart(true);
        return preset;
    }

    private static final class Fixture {
        private final HarnessExtensionsProperties properties = new HarnessExtensionsProperties();
        private final Map<String, HarnessPluginEntity> pluginMap = new HashMap<>();
        private final List<String> runPluginIds;
        private final List<HarnessPluginEntity> plugins;
        private final IInstallHarnessPluginCase installCase;
        private final IRunHarnessPluginCase runCase;
        private final IPluginRegistryService registryService;
        private final IHarnessPluginRepository repository;

        private Fixture(List<HarnessPluginEntity> plugins, List<String> runPluginIds) {
            this.plugins = plugins;
            this.runPluginIds = runPluginIds;
            plugins.forEach(plugin -> pluginMap.put(plugin.getPluginId(), plugin));
            this.installCase = request -> {
                throw new AssertionError("presets should not be installed in this test");
            };
            this.runCase = request -> {
                runPluginIds.add(request.pluginId());
                return new RunPluginResponseDTO(
                        request.pluginId(), 0, "java", null, true, Instant.now(), "java plugin is running");
            };
            this.registryService = new IPluginRegistryService() {
                @Override public PluginInstallationResultVO installPlugin(
                        String pluginId, String displayName, String pluginVersion,
                        String runtimeType, String sourcePath, String entrypoint) {
                    throw new AssertionError("registry installation should not run in this test");
                }
                @Override public Optional<HarnessPluginEntity> loadPluginOptional(String pluginId) {
                    return Optional.ofNullable(pluginMap.get(pluginId));
                }
                @Override public HarnessPluginEntity loadPlugin(String pluginId) {
                    return pluginMap.get(pluginId);
                }
                @Override public PluginInstallStatusEnumVO refreshStatus(
                        HarnessPluginEntity plugin, boolean bridgeReady) {
                    return plugin.getStatus();
                }
                @Override public PluginInstallStatusEnumVO disablePlugin(String pluginId) {
                    return PluginInstallStatusEnumVO.DISABLED;
                }
                @Override public PluginInstallStatusEnumVO enablePlugin(String pluginId) {
                    return PluginInstallStatusEnumVO.ACTIVE;
                }
                @Override public PluginInstallStatusEnumVO uninstallPlugin(String pluginId) {
                    return PluginInstallStatusEnumVO.UNINSTALLED;
                }
            };
            this.repository = new IHarnessPluginRepository() {
                @Override public void save(HarnessPluginEntity plugin) {
                    pluginMap.put(plugin.getPluginId(), plugin);
                }
                @Override public List<HarnessPluginEntity> listAll() {
                    return plugins;
                }
                @Override public Optional<HarnessPluginEntity> findByPluginId(String pluginId) {
                    return Optional.ofNullable(pluginMap.get(pluginId));
                }
                @Override public void deleteByPluginId(String pluginId) {
                    pluginMap.remove(pluginId);
                }
            };
        }
    }
}
