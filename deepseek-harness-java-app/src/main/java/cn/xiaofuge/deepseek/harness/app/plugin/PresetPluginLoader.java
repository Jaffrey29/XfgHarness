package cn.xiaofuge.deepseek.harness.app.plugin;

import cn.xiaofuge.deepseek.harness.cases.plugin.IInstallHarnessPluginCase;
import cn.xiaofuge.deepseek.harness.cases.plugin.IRunHarnessPluginCase;
import cn.xiaofuge.deepseek.harness.api.dto.InstallPluginRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.RunPluginRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.RunPluginResponseDTO;
import cn.xiaofuge.deepseek.harness.domain.plugin.IPluginRegistryService;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.entity.HarnessPluginEntity;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.valobj.PluginInstallStatusEnumVO;
import cn.xiaofuge.deepseek.harness.infrastructure.config.HarnessExtensionsProperties;
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
 * <p>调用流程：Spring Boot 启动 → 加载模型/插件预设 → 注册运行时组件。</p>
 * <p>示例：启动器同步默认模型并装载预置插件。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Configuration
@Order(20)
public class PresetPluginLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PresetPluginLoader.class);

    private final HarnessExtensionsProperties properties;
    private final IInstallHarnessPluginCase installCase;
    private final IRunHarnessPluginCase runCase;
    private final IPluginRegistryService pluginRegistryService;

    /**
     * 初始化预置插件加载器。
     * <p>流程：读取 application.yml 中的预置插件配置 → 调用插件注册服务安装/激活。</p>
     */
    public PresetPluginLoader(
            HarnessExtensionsProperties properties,
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
        if (properties.getPlugins().getPreset() == null || properties.getPlugins().getPreset().isEmpty()) {
            return;
        }
        for (HarnessExtensionsProperties.PresetPlugin preset : properties.getPlugins().getPreset()) {
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
    private void installPreset(HarnessExtensionsProperties.PresetPlugin preset) {
        Optional<HarnessPluginEntity> existing = pluginRegistryService.loadPluginOptional(preset.getPluginId());
        if (existing.isPresent()) {
            HarnessPluginEntity plugin = existing.get();
            if (preset.isAutoStart() && plugin.getStatus() == PluginInstallStatusEnumVO.DISABLED) {
                pluginRegistryService.enablePlugin(preset.getPluginId());
                log.info("Preset plugin '{}' re-enabled", preset.getPluginId());
            }
        }
        else {
            InstallPluginRequestDTO request = new InstallPluginRequestDTO(
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

        if (!preset.isAutoStart()) {
            return;
        }
        RunPluginResponseDTO result = runCase.execute(new RunPluginRequestDTO(preset.getPluginId()));
        if (result.running()) {
            log.info("Preset plugin '{}' restored: {}", preset.getPluginId(), result.message());
        } else {
            log.warn("Preset plugin '{}' failed to restore: {}", preset.getPluginId(), result.message());
        }
    }
}
