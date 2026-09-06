package cn.walioffice.deepseek.harness.domain.plugin.registry.model.valobj;

import java.time.Instant;

/**
 * Returns the result of installing one plugin.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record PluginInstallationResultVO(
        String pluginId,
        PluginRuntimeTypeEnumVO runtimeType,
        PluginInstallModeEnumVO installMode,
        PluginInstallStatusEnumVO status,
        boolean directlyExecutable,
        Instant installedAt
) {
}
