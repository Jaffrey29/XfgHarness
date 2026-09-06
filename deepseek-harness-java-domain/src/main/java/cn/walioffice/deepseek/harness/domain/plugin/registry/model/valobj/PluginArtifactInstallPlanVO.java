package cn.walioffice.deepseek.harness.domain.plugin.registry.model.valobj;

/**
 * Describes where one plugin source was installed inside the managed install root.
 *
 * @param installedSourcePath installed plugin directory
 * @param normalizedEntrypoint entrypoint relative to the installed plugin directory
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record PluginArtifactInstallPlanVO(
        String installedSourcePath,
        String normalizedEntrypoint
) {
}
