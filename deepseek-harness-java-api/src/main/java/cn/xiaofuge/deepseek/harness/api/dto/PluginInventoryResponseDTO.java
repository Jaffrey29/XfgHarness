package cn.xiaofuge.deepseek.harness.api.dto;

import java.time.Instant;
import java.util.List;

public record PluginInventoryResponseDTO(
        Instant generatedAt,
        long count,
        List<PluginInventoryItemDTO> plugins
) {
}
