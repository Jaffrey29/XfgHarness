package cn.xiaofuge.deepseek.harness.api.dto;

import java.time.Instant;
import java.util.Map;

public record SessionEventQueryResponseDTO(
        long seq,
        String eventType,
        Instant timestamp,
        Map<String, Object> data
) {
}
