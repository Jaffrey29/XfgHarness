package cn.xiaofuge.deepseek.harness.domain.plugin;

import cn.xiaofuge.deepseek.harness.domain.plugin.bridge.model.valobj.PluginBridgePlanVO;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.entity.HarnessPluginEntity;
import cn.xiaofuge.deepseek.harness.domain.plugin.runtime.model.valobj.PluginProcessStatusVO;
import cn.xiaofuge.deepseek.harness.domain.plugin.runtime.model.valobj.PluginRunResultVO;

/**
 * Owns plugin process startup and status lookup.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IPluginRuntimeService {

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
