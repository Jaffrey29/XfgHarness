package cn.walioffice.deepseek.harness.domain.plugin.runtime.adapter.port;

import cn.walioffice.deepseek.harness.domain.plugin.bridge.model.valobj.PluginBridgePlanVO;
import cn.walioffice.deepseek.harness.domain.plugin.registry.model.entity.HarnessPluginEntity;
import cn.walioffice.deepseek.harness.domain.plugin.runtime.model.valobj.PluginProcessStatusVO;
import cn.walioffice.deepseek.harness.domain.plugin.runtime.model.valobj.PluginRunResultVO;

/**
 * Starts and inspects plugin sidecar processes.
 *
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
