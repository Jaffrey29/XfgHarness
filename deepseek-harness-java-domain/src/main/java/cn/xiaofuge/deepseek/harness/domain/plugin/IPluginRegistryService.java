package cn.xiaofuge.deepseek.harness.domain.plugin;

import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.entity.HarnessPluginEntity;
import java.util.Optional;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.valobj.PluginInstallStatusEnumVO;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.valobj.PluginInstallationResultVO;

/**
 * Owns plugin registration and plugin lifecycle status persistence.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IPluginRegistryService {

    PluginInstallationResultVO installPlugin(
            String pluginId,
            String displayName,
            String pluginVersion,
            String runtimeType,
            String sourcePath,
            String entrypoint
    );

    HarnessPluginEntity loadPlugin(String pluginId);

    PluginInstallStatusEnumVO refreshStatus(HarnessPluginEntity plugin, boolean bridgeReady);

    /**
     * Disables one installed plugin — keeps artifacts on disk but marks it inactive.
     *
     * @param pluginId plugin identifier
     * @return resulting status
     */
    PluginInstallStatusEnumVO disablePlugin(String pluginId);

    /**
     * Re-enables one disabled plugin — back to registered state.
     *
     * @param pluginId plugin identifier
     * @return resulting status
     */
    PluginInstallStatusEnumVO enablePlugin(String pluginId);

    /**
     * Uninstalls one plugin — removes artifacts and marks the registration row as uninstalled.
     *
     * @param pluginId plugin identifier
     * @return resulting status
     */
    PluginInstallStatusEnumVO uninstallPlugin(String pluginId);

    /**
     * Loads one plugin without throwing when absent.
     *
     * @param pluginId plugin identifier
     * @return plugin entity when present
     */
    Optional<HarnessPluginEntity> loadPluginOptional(String pluginId);
}
