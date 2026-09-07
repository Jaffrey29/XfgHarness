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
 * <p>调用流程：Controller/API → 插件用例 → 领域插件服务 → PluginRuntime/Bridge。</p>
 * <p>示例：安装插件后先激活，再通过统一工具名调用插件工具。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class ManageHarnessPluginCaseImpl implements IManageHarnessPluginCase {

    private final IPluginRegistryService pluginRegistryService;
    private final IPluginRuntimeService pluginRuntimeService;

    /**
     * 初始化插件生命周期管理用例。
     * <p>流程：接收启用/停用/卸载命令 → 委托插件领域服务执行。</p>
     */
    public ManageHarnessPluginCaseImpl(
            IPluginRegistryService pluginRegistryService,
            IPluginRuntimeService pluginRuntimeService
    ) {
        this.pluginRegistryService = pluginRegistryService;
        this.pluginRuntimeService = pluginRuntimeService;
    }

    /**
     * enable。
     * 流程：读取命令 → 执行用例编排 → 返回结果。
     * @param pluginId 插件 ID。
     * @return 方法执行结果。
     */

    @Override
    public PluginLifecycleResult disable(String pluginId) {
        pluginRuntimeService.stop(pluginId);
        PluginInstallStatusEnumVO status = pluginRegistryService.disablePlugin(pluginId);
        return new PluginLifecycleResult(pluginId, status.name(), "plugin disabled", Instant.now());
    }

    /**
     * uninstall。
     * 流程：读取命令 → 执行用例编排 → 返回结果。
     * @param pluginId 插件 ID。
     * @return 方法执行结果。
     */

    @Override
    public PluginLifecycleResult enable(String pluginId) {
        PluginInstallStatusEnumVO status = pluginRegistryService.enablePlugin(pluginId);
        return new PluginLifecycleResult(pluginId, status.name(), "plugin enabled", Instant.now());
    }

    /**
     * 停止目标对象。
     * 流程：读取命令 → 执行用例编排 → 返回结果。
     * @param pluginId 插件 ID。
     * @return 方法执行结果。
     */

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
