package cn.xiaofuge.deepseek.harness.domain.plugin.runtime.adapter.port;

import cn.xiaofuge.deepseek.harness.domain.plugin.bridge.model.valobj.PluginBridgePlanVO;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.entity.HarnessPluginEntity;
import cn.xiaofuge.deepseek.harness.domain.plugin.runtime.model.valobj.PluginProcessStatusVO;
import cn.xiaofuge.deepseek.harness.domain.plugin.runtime.model.valobj.PluginRunResultVO;

/**
 * Starts and inspects plugin sidecar processes.
 *
 * <p>调用流程：插件安装/激活 → Bridge 转换 → Runtime 执行 → Hook/事件回传。</p>
 * <p>示例：Java SPI 插件注册工具后，Agent 可通过工具名调用其 run 方法。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IPluginProcessPort {

    PluginRunResultVO start(HarnessPluginEntity plugin, PluginBridgePlanVO plan);

    PluginProcessStatusVO status(String pluginId);

    /**
     * Stops one running plugin sidecar process.
     *
     * @param pluginId plugin identifier
     * @return resulting status after stop attempt
     */
    PluginProcessStatusVO stop(String pluginId);
}
