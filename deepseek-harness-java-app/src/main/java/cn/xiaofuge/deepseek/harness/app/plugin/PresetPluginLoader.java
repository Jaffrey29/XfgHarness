package cn.xiaofuge.deepseek.harness.app.plugin;

import cn.xiaofuge.deepseek.harness.cases.plugin.InstallPluginCommand;
import cn.xiaofuge.deepseek.harness.cases.plugin.IInstallHarnessPluginCase;
import cn.xiaofuge.deepseek.harness.cases.plugin.IRunHarnessPluginCase;
import cn.xiaofuge.deepseek.harness.cases.plugin.RunPluginCommand;
import cn.xiaofuge.deepseek.harness.cases.plugin.RunPluginResult;
import cn.xiaofuge.deepseek.harness.domain.plugin.IPluginRegistryService;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.entity.HarnessPluginEntity;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.valobj.PluginInstallStatusEnumVO;
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
    private final IRunHarnessPluginCase runCase;
    private final IPluginRegistryService pluginRegistryService;

    /**
     * 处理Preset Plugin Loader相关逻辑。
     */
    public PresetPluginLoader(
            PresetPluginProperties properties,
            IInstallHarnessPluginCase installCase,
            IRunHarnessPluginCase runCase,
            IPluginRegistryService pluginRegistryService
    ) {
        this.properties = properties;
        this.installCase = installCase;
        this.runCase = runCase;
        this.pluginRegistryService = pluginRegistryService;
    }

    /**
     * 运行。
     */
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

    /**
     * 安装组件，写入登记信息并返回安装结果。
     */
    private void installPreset(PresetPluginProperties.PresetPlugin preset) {
        Optional<HarnessPluginEntity> existing = pluginRegistryService.loadPluginOptional(preset.getPluginId());
        if (existing.isPresent()) {
            HarnessPluginEntity plugin = existing.get();
            if (preset.isAutoStart() && plugin.getStatus() == PluginInstallStatusEnumVO.DISABLED) {
                pluginRegistryService.enablePlugin(preset.getPluginId());
                log.info("Preset plugin '{}' re-enabled", preset.getPluginId());
            }
        }
        else {
            InstallPluginCommand cmd = new InstallPluginCommand(
                    preset.getPluginId(),
                    preset.getDisplayName(),
                    preset.getPluginVersion(),
                    preset.getRuntimeType(),
                    preset.getSourcePath(),
                    preset.getEntrypoint()
            );
            installCase.execute(cmd);
            log.info("Preset plugin '{}' installed from {}", preset.getPluginId(), preset.getSourcePath());
        }

        if (!preset.isAutoStart()) {
            return;
        }
        RunPluginResult result = runCase.execute(new RunPluginCommand(preset.getPluginId()));
        if (result.running()) {
            log.info("Preset plugin '{}' restored: {}", preset.getPluginId(), result.message());
        } else {
            log.warn("Preset plugin '{}' failed to restore: {}", preset.getPluginId(), result.message());
        }
    }
}
