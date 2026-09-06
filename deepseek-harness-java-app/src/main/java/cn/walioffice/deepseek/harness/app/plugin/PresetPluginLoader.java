package cn.walioffice.deepseek.harness.app.plugin;

import cn.walioffice.deepseek.harness.api.dto.plugin.InstallPluginRequest;
import cn.walioffice.deepseek.harness.cases.plugin.install.IInstallHarnessPluginCase;
import cn.walioffice.deepseek.harness.domain.plugin.IPluginRegistryService;
import cn.walioffice.deepseek.harness.domain.plugin.registry.model.entity.HarnessPluginEntity;
import cn.walioffice.deepseek.harness.domain.plugin.registry.model.valobj.PluginInstallStatusEnumVO;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * 启动时安装 application.yml 中尚未注册的预置插件；
 * 如果请求 auto-enable，还会重新启用处于 DISABLED 状态的预置插件。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Configuration
@Order(20)
public class PresetPluginLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PresetPluginLoader.class);

    private final PresetPluginProperties properties;
    private final IInstallHarnessPluginCase installCase;
    private final IPluginRegistryService pluginRegistryService;

    public PresetPluginLoader(
            PresetPluginProperties properties,
            IInstallHarnessPluginCase installCase,
            IPluginRegistryService pluginRegistryService
    ) {
        this.properties = properties;
        this.installCase = installCase;
        this.pluginRegistryService = pluginRegistryService;
    }

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        if (properties.getPlugins() == null || properties.getPlugins().isEmpty()) {
            return;
        }
        for (PresetPluginProperties.PresetPlugin preset : properties.getPlugins()) {
            try {
                installPreset(preset);
            } catch (Exception e) {
                log.warn("Preset plugin '{}' install skipped: {}", preset.getPluginId(), e.getMessage());
            }
        }
    }

    private void installPreset(PresetPluginProperties.PresetPlugin preset) {
        Optional<HarnessPluginEntity> existing = pluginRegistryService.loadPluginOptional(preset.getPluginId());
        if (existing.isPresent()) {
            HarnessPluginEntity plugin = existing.get();
            if (preset.isAutoEnable() && plugin.getStatus() == PluginInstallStatusEnumVO.DISABLED) {
                pluginRegistryService.enablePlugin(preset.getPluginId());
                log.info("Preset plugin '{}' re-enabled", preset.getPluginId());
            }
            return;
        }
        InstallPluginRequest request = new InstallPluginRequest(
                preset.getPluginId(),
                preset.getDisplayName(),
                preset.getPluginVersion(),
                preset.getRuntimeType(),
                preset.getSourcePath(),
                preset.getEntrypoint()
        );
        installCase.execute(request);
        log.info("Preset plugin '{}' installed from {}", preset.getPluginId(), preset.getSourcePath());
    }
}
