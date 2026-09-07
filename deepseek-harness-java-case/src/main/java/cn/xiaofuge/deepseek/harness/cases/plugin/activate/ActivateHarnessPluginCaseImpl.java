package cn.xiaofuge.deepseek.harness.cases.plugin.activate;

import cn.xiaofuge.deepseek.harness.cases.plugin.ActivatePluginCommand;
import cn.xiaofuge.deepseek.harness.cases.plugin.ActivatePluginResult;
import cn.xiaofuge.deepseek.harness.cases.plugin.IActivateHarnessPluginCase;
import cn.xiaofuge.deepseek.harness.domain.plugin.bridge.model.valobj.PluginActivationResultVO;
import cn.xiaofuge.deepseek.harness.domain.plugin.bridge.model.valobj.PluginBridgePlanVO;
import cn.xiaofuge.deepseek.harness.domain.plugin.IPluginBridgeService;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.entity.HarnessPluginEntity;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.valobj.PluginInstallStatusEnumVO;
import cn.xiaofuge.deepseek.harness.domain.plugin.IPluginRegistryService;
import java.time.Instant;
import org.springframework.stereotype.Service;

/**
 * 协调插件激活纵切面。
 *
 * <p>调用流程：Controller/API → 插件用例 → 领域插件服务 → PluginRuntime/Bridge。</p>
 * <p>示例：安装插件后先激活，再通过统一工具名调用插件工具。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class ActivateHarnessPluginCaseImpl implements IActivateHarnessPluginCase {

    private final IPluginRegistryService pluginRegistryService;
    private final IPluginBridgeService pluginBridgeService;

    /**
     * 激活组件，准备其运行时能力。
     */
    public ActivateHarnessPluginCaseImpl(
            IPluginRegistryService pluginRegistryService,
            IPluginBridgeService pluginBridgeService
    ) {
        this.pluginRegistryService = pluginRegistryService;
        this.pluginBridgeService = pluginBridgeService;
    }

    @Override
    public ActivatePluginResult execute(ActivatePluginCommand request) {
        HarnessPluginEntity plugin = pluginRegistryService.loadPlugin(request.pluginId());
        PluginBridgePlanVO plan = pluginBridgeService.inspect(plugin);
        Instant checkedAt = Instant.now();
        PluginInstallStatusEnumVO status = pluginRegistryService.refreshStatus(plugin, plan.bridgeReady());
        pluginBridgeService.recordBinding(plugin.getPluginId(), plan, checkedAt);
        PluginActivationResultVO result = new PluginActivationResultVO(
                plugin.getPluginId(),
                status,
                plan.packageType(),
                plan.runtimeCommand(),
                plan.bridgeReady(),
                plan.message(),
                checkedAt
        );
        return new ActivatePluginResult(
                result.pluginId(),
                result.status().name(),
                result.packageType().name(),
                result.runtimeCommand(),
                result.bridgeReady(),
                result.message(),
                result.checkedAt()
        );
    }
}
