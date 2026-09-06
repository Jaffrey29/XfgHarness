package cn.walioffice.deepseek.harness.api.dto.session;

import java.time.Instant;
import java.util.Map;

/**
 * 会话事件查询响应。
 *
 * @param seq event sequence number
 * @param eventType event type
 * @param timestamp event timestamp
 * @param data event payload
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record SessionEventQueryResponse(
        long seq,
        String eventType,
        Instant timestamp,
        Map<String, Object> data
) {
}
