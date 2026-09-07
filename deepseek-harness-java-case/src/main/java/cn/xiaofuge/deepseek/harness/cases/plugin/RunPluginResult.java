package cn.xiaofuge.deepseek.harness.cases.plugin;

import java.time.Instant;

/**
 * 插件进程启动结果。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record RunPluginResult(
        String pluginId,
        long processId,
        String runtimeCommand,
        String workingDirectory,
        boolean running,
        Instant startedAt,
        String message
) {
}
