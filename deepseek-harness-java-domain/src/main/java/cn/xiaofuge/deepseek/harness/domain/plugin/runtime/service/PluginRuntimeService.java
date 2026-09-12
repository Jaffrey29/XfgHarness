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
 * <p>调用流程：插件安装/激活 → Bridge 转换 → Runtime 执行 → Hook/事件回传。</p>
 * <p>示例：Java SPI 插件注册工具后，Agent 可通过工具名调用其 run 方法。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class PluginRuntimeService implements IPluginRuntimeService {

    private final IPluginProcessPort pluginProcessPort;

    /**
     * 处理插件 运行时 Service。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param pluginProcessPort 插件 Process Port参数。
     * @return 方法执行结果。
     */
    public PluginRuntimeService(IPluginProcessPort pluginProcessPort) {
        this.pluginProcessPort = pluginProcessPort;
    }

    @Override
    public PluginRunResultVO start(HarnessPluginEntity plugin, PluginBridgePlanVO plan) {
        if (plan.entrypointPath() == null || plan.entrypointPath().isBlank()) {
            throw new IllegalStateException("plugin bridge plan does not provide a runnable entrypoint");
        }
        return pluginProcessPort.start(plugin, plan);
    }
    /**
     * 返回 status。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param pluginId 插件 ID。
     * @return 方法执行结果。
     */


    @Override
    public PluginProcessStatusVO status(String pluginId) {
        return pluginProcessPort.status(pluginId);
    }
    /**
     * 停止目标对象。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param pluginId 插件 ID。
     * @return 方法执行结果。
     */


    @Override
    public PluginProcessStatusVO stop(String pluginId) {
        return pluginProcessPort.stop(pluginId);
    }
}
