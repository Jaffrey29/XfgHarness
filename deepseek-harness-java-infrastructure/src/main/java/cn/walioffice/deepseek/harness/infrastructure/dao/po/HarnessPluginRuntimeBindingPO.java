package cn.walioffice.deepseek.harness.infrastructure.dao.po;

import java.time.Instant;

/**
 * 保存某插件最近一次桥接解析结果。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record HarnessPluginRuntimeBindingPO(
        String pluginId,
        String packageType,
        String manifestPath,
        String workingDirectory,
        String runtimeCommand,
        boolean bridgeReady,
        String message,
        Instant checkedAt
) {
}
