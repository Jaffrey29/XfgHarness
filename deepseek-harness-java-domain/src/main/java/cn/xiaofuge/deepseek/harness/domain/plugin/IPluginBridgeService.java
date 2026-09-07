package cn.xiaofuge.deepseek.harness.domain.plugin;

import cn.xiaofuge.deepseek.harness.domain.plugin.bridge.model.valobj.PluginBridgePlanVO;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.entity.HarnessPluginEntity;
import java.time.Instant;

/**
 * Owns plugin bridge inspection and binding persistence.
 *
 * <p>调用流程：插件安装/激活 → Bridge 转换 → Runtime 执行 → Hook/事件回传。</p>
 * <p>示例：Java SPI 插件注册工具后，Agent 可通过工具名调用其 run 方法。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IPluginBridgeService {

    PluginBridgePlanVO inspect(HarnessPluginEntity plugin);

    void recordBinding(String pluginId, PluginBridgePlanVO plan, Instant checkedAt);
}
