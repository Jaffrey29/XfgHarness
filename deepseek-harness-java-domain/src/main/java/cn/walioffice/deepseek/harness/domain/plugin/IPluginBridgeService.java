package cn.walioffice.deepseek.harness.domain.plugin;

import cn.walioffice.deepseek.harness.domain.plugin.bridge.model.valobj.PluginBridgePlanVO;
import cn.walioffice.deepseek.harness.domain.plugin.registry.model.entity.HarnessPluginEntity;
import java.time.Instant;

/**
 * Owns plugin bridge inspection and binding persistence.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IPluginBridgeService {

    PluginBridgePlanVO inspect(HarnessPluginEntity plugin);

    void recordBinding(String pluginId, PluginBridgePlanVO plan, Instant checkedAt);
}
