package cn.walioffice.deepseek.harness.domain.plugin.runtime.model.valobj;

import java.time.Instant;

/**
 * Returns the result of starting one plugin process.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record PluginRunResultVO(
        String pluginId,
        long processId,
        String runtimeCommand,
        String workingDirectory,
        boolean running,
        Instant startedAt,
        String message
) {
}
