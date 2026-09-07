package cn.xiaofuge.deepseek.harness.domain.plugin.registry.adapter.port;

import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.valobj.PluginArtifactInstallPlanVO;

/**
 * Materializes one plugin source into the managed install root.
 *
 * <p>调用流程：插件安装/激活 → Bridge 转换 → Runtime 执行 → Hook/事件回传。</p>
 * <p>示例：Java SPI 插件注册工具后，Agent 可通过工具名调用其 run 方法。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IPluginArtifactInstallerPort {

    /**
     * Copies one plugin source into the managed install root and normalizes its entrypoint.
     *
     * @param pluginId stable plugin identifier
     * @param sourcePath local plugin source directory
     * @param entrypoint plugin entrypoint path relative to the source directory, or absolute within it
     * @return installed source directory and normalized entrypoint
     */
    PluginArtifactInstallPlanVO install(String pluginId, String sourcePath, String entrypoint);

    /**
     * Removes one plugin's installed artifacts from the managed install root.
     *
     * @param pluginId stable plugin identifier
     */
    void uninstall(String pluginId);
}
