package cn.xiaofuge.deepseek.harness.cases.plugin.manage;

import cn.xiaofuge.deepseek.harness.cases.plugin.PluginLifecycleResult;
import cn.xiaofuge.deepseek.harness.cases.plugin.IManageHarnessPluginCase;
import cn.xiaofuge.deepseek.harness.domain.plugin.IPluginRegistryService;
import cn.xiaofuge.deepseek.harness.domain.plugin.IPluginRuntimeService;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.valobj.PluginInstallStatusEnumVO;
import cn.xiaofuge.deepseek.harness.domain.plugin.runtime.model.valobj.PluginProcessStatusVO;
import java.time.Instant;
import org.springframework.stereotype.Service;

/**
 * 协调插件生命周期管理纵切面。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class ManageHarnessPluginCaseImpl implements IManageHarnessPluginCase {

    private final IPluginRegistryService pluginRegistryService;
    private final IPluginRuntimeService pluginRuntimeService;

    /**
     * 处理Manage Harness Plugin Case Impl相关逻辑。
     */
    public ManageHarnessPluginCaseImpl(
            IPluginRegistryService pluginRegistryService,
            IPluginRuntimeService pluginRuntimeService
    ) {
        this.pluginRegistryService = pluginRegistryService;
        this.pluginRuntimeService = pluginRuntimeService;
    }

    @Override
    public PluginLifecycleResult disable(String pluginId) {
        pluginRuntimeService.stop(pluginId);
        PluginInstallStatusEnumVO status = pluginRegistryService.disablePlugin(pluginId);
        return new PluginLifecycleResult(pluginId, status.name(), "plugin disabled", Instant.now());
    }

    @Override
    public PluginLifecycleResult enable(String pluginId) {
        PluginInstallStatusEnumVO status = pluginRegistryService.enablePlugin(pluginId);
        return new PluginLifecycleResult(pluginId, status.name(), "plugin enabled", Instant.now());
    }

    @Override
    public PluginLifecycleResult uninstall(String pluginId) {
        pluginRuntimeService.stop(pluginId);
        PluginInstallStatusEnumVO status = pluginRegistryService.uninstallPlugin(pluginId);
        return new PluginLifecycleResult(pluginId, status.name(), "plugin uninstalled", Instant.now());
    }

    @Override
    public PluginLifecycleResult stop(String pluginId) {
        PluginProcessStatusVO result = pluginRuntimeService.stop(pluginId);
        return new PluginLifecycleResult(pluginId, result.running() ? "RUNNING" : "STOPPED", result.message(), Instant.now());
    }
}
