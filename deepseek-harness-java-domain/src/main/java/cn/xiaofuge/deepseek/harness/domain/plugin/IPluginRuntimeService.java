package cn.xiaofuge.deepseek.harness.domain.plugin;

import cn.xiaofuge.deepseek.harness.domain.plugin.bridge.model.valobj.PluginBridgePlanVO;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.entity.HarnessPluginEntity;
import cn.xiaofuge.deepseek.harness.domain.plugin.runtime.model.valobj.PluginProcessStatusVO;
import cn.xiaofuge.deepseek.harness.domain.plugin.runtime.model.valobj.PluginRunResultVO;

/**
 * Owns plugin process startup and status lookup.
 *
 * <p>调用流程：插件安装/激活 → Bridge 转换 → Runtime 执行 → Hook/事件回传。</p>
 * <p>示例：Java SPI 插件注册工具后，Agent 可通过工具名调用其 run 方法。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IPluginRuntimeService {

    PluginRunResultVO start(HarnessPluginEntity plugin, PluginBridgePlanVO plan);

    PluginProcessStatusVO status(String pluginId);

    /**
     * 停止生命周期或异步任务。
     * <p>流程：校验入参 → 执行领域动作 → 返回处理结果。</p>
     * @param pluginId 插件ID。
     * @return 领域处理结果。
     */
    PluginProcessStatusVO stop(String pluginId);
}
