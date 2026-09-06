package cn.walioffice.deepseek.harness.api.dto.plugin;

import java.time.Instant;

/**
 * 插件进程状态查询响应。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record QueryPluginStatusResponse(
        String pluginId,
        Long processId,
        boolean running,
        String runtimeCommand,
        String workingDirectory,
        Instant startedAt,
        String message
) {
}
