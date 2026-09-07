package cn.xiaofuge.deepseek.harness.api.dto;

import java.time.Instant;

public record PluginLifecycleResponseDTO(
        String pluginId,
        String status,
        String message,
        Instant updatedAt
) {
}
