package cn.xiaofuge.deepseek.harness.cases.session;

import java.time.Instant;
import java.util.Map;

/**
 * 会话事件查询视图。
 *
 * @param seq event sequence number
 * @param eventType event type
 * @param timestamp event timestamp
 * @param data event payload
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record SessionEventView(
        long seq,
        String eventType,
        Instant timestamp,
        Map<String, Object> data
) {
}
