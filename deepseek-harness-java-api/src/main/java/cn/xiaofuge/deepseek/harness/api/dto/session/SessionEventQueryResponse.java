package cn.xiaofuge.deepseek.harness.api.dto.session;

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
 * <p>调用流程：Controller/DTO → 应用门面 → 用例；响应对象由用例结果转换。</p>
 * <p>示例：DTO 只做协议隔离，不承载领域规则。</p>
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
