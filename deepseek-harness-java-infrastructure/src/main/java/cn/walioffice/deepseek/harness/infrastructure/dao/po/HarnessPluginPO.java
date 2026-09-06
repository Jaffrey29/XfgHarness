package cn.walioffice.deepseek.harness.infrastructure.dao.po;

import java.time.Instant;

/**
 * 保存一条插件安装记录。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record HarnessPluginPO(
        String pluginId,
        String displayName,
        String pluginVersion,
        String runtimeType,
        String installMode,
        String sourcePath,
        String entrypoint,
        String status,
        Instant installedAt
) {
}
