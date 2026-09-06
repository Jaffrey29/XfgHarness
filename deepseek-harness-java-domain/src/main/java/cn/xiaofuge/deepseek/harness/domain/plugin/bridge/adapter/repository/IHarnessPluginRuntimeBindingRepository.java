package cn.xiaofuge.deepseek.harness.domain.plugin.bridge.adapter.repository;

import cn.xiaofuge.deepseek.harness.domain.plugin.bridge.model.valobj.PluginBridgePlanVO;
import java.time.Instant;

/**
 * Persists the last bridge-resolution result for one plugin.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IHarnessPluginRuntimeBindingRepository {

    void save(String pluginId, PluginBridgePlanVO plan, Instant checkedAt);
}
