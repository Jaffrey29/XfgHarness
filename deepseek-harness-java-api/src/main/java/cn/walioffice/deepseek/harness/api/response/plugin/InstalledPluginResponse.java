package cn.walioffice.deepseek.harness.api.response.plugin;

import java.time.Instant;

/**
 * 返回 Web 控制台使用的一条已安装插件记录。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record InstalledPluginResponse(
        String pluginId,
        String displayName,
        String pluginVersion,
        String runtimeType,
        String installMode,
        String status,
        String sourcePath,
        String entrypoint,
        Instant installedAt
) {
}
