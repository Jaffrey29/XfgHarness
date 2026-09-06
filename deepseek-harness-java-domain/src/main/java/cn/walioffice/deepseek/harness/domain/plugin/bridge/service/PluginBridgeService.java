package cn.walioffice.deepseek.harness.domain.plugin.bridge.service;

import cn.walioffice.deepseek.harness.domain.plugin.IPluginBridgeService;
import cn.walioffice.deepseek.harness.domain.plugin.bridge.adapter.port.IPluginRuntimeBridgePort;
import cn.walioffice.deepseek.harness.domain.plugin.bridge.adapter.repository.IHarnessPluginRuntimeBindingRepository;
import cn.walioffice.deepseek.harness.domain.plugin.bridge.model.valobj.PluginBridgePlanVO;
import cn.walioffice.deepseek.harness.domain.plugin.registry.model.entity.HarnessPluginEntity;
import java.time.Instant;

/**
 * Owns plugin bridge inspection and binding persistence.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class PluginBridgeService implements IPluginBridgeService {

    private final IPluginRuntimeBridgePort pluginRuntimeBridgePort;
    private final IHarnessPluginRuntimeBindingRepository harnessPluginRuntimeBindingRepository;

    public PluginBridgeService(
            IPluginRuntimeBridgePort pluginRuntimeBridgePort,
            IHarnessPluginRuntimeBindingRepository harnessPluginRuntimeBindingRepository
    ) {
        this.pluginRuntimeBridgePort = pluginRuntimeBridgePort;
        this.harnessPluginRuntimeBindingRepository = harnessPluginRuntimeBindingRepository;
    }

    @Override
    public PluginBridgePlanVO inspect(HarnessPluginEntity plugin) {
        return pluginRuntimeBridgePort.inspect(plugin);
    }

    @Override
    public void recordBinding(String pluginId, PluginBridgePlanVO plan, Instant checkedAt) {
        harnessPluginRuntimeBindingRepository.save(pluginId, plan, checkedAt);
    }
}
