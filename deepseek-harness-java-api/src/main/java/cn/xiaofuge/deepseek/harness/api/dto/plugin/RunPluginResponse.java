package cn.xiaofuge.deepseek.harness.api.dto.plugin;

import java.time.Instant;

/**
 * 插件进程启动响应。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record RunPluginResponse(
        String pluginId,
        long processId,
        String runtimeCommand,
        String workingDirectory,
        boolean running,
        Instant startedAt,
        String message
) {
}
