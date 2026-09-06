package cn.walioffice.deepseek.harness.domain.plugin.bridge.model.valobj;

/**
 * Describes how one installed plugin can be activated or bridged.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record PluginBridgePlanVO(
        PluginPackageTypeEnumVO packageType,
        String manifestPath,
        String workingDirectory,
        String entrypointPath,
        String runtimeCommand,
        boolean bridgeReady,
        String message
) {
}
