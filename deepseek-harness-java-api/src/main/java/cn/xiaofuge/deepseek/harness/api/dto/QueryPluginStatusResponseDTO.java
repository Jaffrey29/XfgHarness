package cn.xiaofuge.deepseek.harness.api.dto;

import java.time.Instant;

public record QueryPluginStatusResponseDTO(
        String pluginId,
        Long processId,
        boolean running,
        String runtimeCommand,
        String workingDirectory,
        Instant startedAt,
        String message
) {
}
