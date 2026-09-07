package cn.xiaofuge.deepseek.harness.domain.plugin;

import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.entity.HarnessPluginEntity;
import java.util.Optional;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.valobj.PluginInstallStatusEnumVO;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.valobj.PluginInstallationResultVO;

/**
 * Owns plugin registration and plugin lifecycle status persistence.
 *
 * <p>调用流程：插件安装/激活 → Bridge 转换 → Runtime 执行 → Hook/事件回传。</p>
 * <p>示例：Java SPI 插件注册工具后，Agent 可通过工具名调用其 run 方法。</p>
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
