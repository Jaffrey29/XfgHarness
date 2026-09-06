package cn.walioffice.deepseek.harness.domain.plugin.bridge.model.valobj;

import cn.walioffice.deepseek.harness.domain.plugin.registry.model.valobj.PluginInstallStatusEnumVO;
import java.time.Instant;

/**
 * Returns the result of activating one installed plugin.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record PluginActivationResultVO(
        String pluginId,
        PluginInstallStatusEnumVO status,
        PluginPackageTypeEnumVO packageType,
        String runtimeCommand,
        boolean bridgeReady,
        String message,
        Instant checkedAt
) {
}
