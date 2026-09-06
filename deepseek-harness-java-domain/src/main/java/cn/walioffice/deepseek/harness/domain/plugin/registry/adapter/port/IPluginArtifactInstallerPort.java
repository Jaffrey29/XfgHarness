package cn.walioffice.deepseek.harness.domain.plugin.registry.adapter.port;

import cn.walioffice.deepseek.harness.domain.plugin.registry.model.valobj.PluginArtifactInstallPlanVO;

/**
 * Materializes one plugin source into the managed install root.
 *
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
