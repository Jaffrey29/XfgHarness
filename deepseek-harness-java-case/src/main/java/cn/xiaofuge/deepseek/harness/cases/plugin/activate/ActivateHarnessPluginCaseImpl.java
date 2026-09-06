package cn.xiaofuge.deepseek.harness.cases.plugin.activate;

import cn.xiaofuge.deepseek.harness.api.dto.plugin.ActivatePluginRequest;
import cn.xiaofuge.deepseek.harness.api.dto.plugin.ActivatePluginResponse;
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
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class ActivateHarnessPluginCaseImpl implements IActivateHarnessPluginCase {

    private final IPluginRegistryService pluginRegistryService;
    private final IPluginBridgeService pluginBridgeService;

    public ActivateHarnessPluginCaseImpl(
            IPluginRegistryService pluginRegistryService,
            IPluginBridgeService pluginBridgeService
    ) {
        this.pluginRegistryService = pluginRegistryService;
        this.pluginBridgeService = pluginBridgeService;
    }

    @Override
    public ActivatePluginResponse execute(ActivatePluginRequest request) {
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
        return new ActivatePluginResponse(
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
