package cn.xiaofuge.deepseek.harness.cases.plugin.manage;

import cn.xiaofuge.deepseek.harness.api.dto.plugin.PluginLifecycleResponse;
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

    public ManageHarnessPluginCaseImpl(
            IPluginRegistryService pluginRegistryService,
            IPluginRuntimeService pluginRuntimeService
    ) {
        this.pluginRegistryService = pluginRegistryService;
        this.pluginRuntimeService = pluginRuntimeService;
    }

    @Override
    public PluginLifecycleResponse disable(String pluginId) {
        pluginRuntimeService.stop(pluginId);
        PluginInstallStatusEnumVO status = pluginRegistryService.disablePlugin(pluginId);
        return new PluginLifecycleResponse(pluginId, status.name(), "plugin disabled", Instant.now());
    }

    @Override
    public PluginLifecycleResponse enable(String pluginId) {
        PluginInstallStatusEnumVO status = pluginRegistryService.enablePlugin(pluginId);
        return new PluginLifecycleResponse(pluginId, status.name(), "plugin enabled", Instant.now());
    }

    @Override
    public PluginLifecycleResponse uninstall(String pluginId) {
        pluginRuntimeService.stop(pluginId);
        PluginInstallStatusEnumVO status = pluginRegistryService.uninstallPlugin(pluginId);
        return new PluginLifecycleResponse(pluginId, status.name(), "plugin uninstalled", Instant.now());
    }

    @Override
    public PluginLifecycleResponse stop(String pluginId) {
        PluginProcessStatusVO result = pluginRuntimeService.stop(pluginId);
        return new PluginLifecycleResponse(pluginId, result.running() ? "RUNNING" : "STOPPED", result.message(), Instant.now());
    }
}
