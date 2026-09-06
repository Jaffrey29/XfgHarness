package cn.xiaofuge.deepseek.harness.domain.plugin.runtime.model.valobj;

import java.time.Instant;

/**
 * Returns the current process status for one plugin.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record PluginProcessStatusVO(
        String pluginId,
        Long processId,
        boolean running,
        String runtimeCommand,
        String workingDirectory,
        Instant startedAt,
        String message
) {
}
