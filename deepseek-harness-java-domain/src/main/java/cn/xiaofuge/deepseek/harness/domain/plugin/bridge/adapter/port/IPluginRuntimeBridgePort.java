package cn.xiaofuge.deepseek.harness.domain.plugin.bridge.adapter.port;

import cn.xiaofuge.deepseek.harness.domain.plugin.bridge.model.valobj.PluginBridgePlanVO;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.entity.HarnessPluginEntity;

/**
 * Resolves bridge execution details for one installed plugin.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IPluginRuntimeBridgePort {

    PluginBridgePlanVO inspect(HarnessPluginEntity plugin);
}
