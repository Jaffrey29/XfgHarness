package cn.xiaofuge.deepseek.harness.domain.plugin.runtime.service;

import cn.xiaofuge.deepseek.harness.domain.plugin.IPluginRuntimeService;
import cn.xiaofuge.deepseek.harness.domain.plugin.bridge.model.valobj.PluginBridgePlanVO;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.entity.HarnessPluginEntity;
import cn.xiaofuge.deepseek.harness.domain.plugin.runtime.adapter.port.IPluginProcessPort;
import cn.xiaofuge.deepseek.harness.domain.plugin.runtime.model.valobj.PluginProcessStatusVO;
import cn.xiaofuge.deepseek.harness.domain.plugin.runtime.model.valobj.PluginRunResultVO;
import org.springframework.stereotype.Service;

/**
 * Owns plugin runtime execution.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class PluginRuntimeService implements IPluginRuntimeService {

    private final IPluginProcessPort pluginProcessPort;

    /**
     * 处理Plugin Runtime Service相关逻辑。
     */
    public PluginRuntimeService(IPluginProcessPort pluginProcessPort) {
        this.pluginProcessPort = pluginProcessPort;
    }

    @Override
    public PluginRunResultVO start(HarnessPluginEntity plugin, PluginBridgePlanVO plan) {
        if (plan.entrypointPath() == null || plan.entrypointPath().isBlank()) {
            throw new IllegalStateException("plugin bridge plan does not provide a runnable Node entrypoint");
        }
        return pluginProcessPort.start(plugin, plan);
    }

    @Override
    public PluginProcessStatusVO status(String pluginId) {
        return pluginProcessPort.status(pluginId);
    }

    @Override
    public PluginProcessStatusVO stop(String pluginId) {
        return pluginProcessPort.stop(pluginId);
    }
}
