package cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.valobj;

/**
 * PluginArtifactInstallPlanVO 表示PluginArtifactInstallPlan的领域值对象。
 */
public record PluginArtifactInstallPlanVO(
        String installedSourcePath,
        String normalizedEntrypoint
) {
}
