package cn.xiaofuge.deepseek.harness.cases.plugin;

import java.time.Instant;

/**
 * 插件进程状态查询结果。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record QueryPluginStatusResult(
        String pluginId,
        Long processId,
        boolean running,
        String runtimeCommand,
        String workingDirectory,
        Instant startedAt,
        String message
) {
}
