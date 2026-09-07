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
 * <p>调用流程：Controller/API → 会话用例 → SessionQueryService/SessionRebuilderService → SessionEventStore。</p>
 * <p>示例：按 sessionId 查询事件后，可重建完整消息时间线。</p>
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
