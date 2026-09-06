package cn.xiaofuge.deepseek.harness.infrastructure.dao.po;

import java.time.Instant;

/**
 * 新事件日志表中一条会话事件的持久化记录。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record SessionEventRowPO(
        Long id,
        String sessionId,
        long seq,
        String eventType,
        Instant eventTime,
        String payloadJson,
        String surfaceOp,
        String sourceEventSeqs
) {
    public SessionEventRowPO(
            String sessionId, long seq, String eventType, Instant eventTime,
            String payloadJson, String surfaceOp, String sourceEventSeqs
    ) {
        this(null, sessionId, seq, eventType, eventTime, payloadJson, surfaceOp, sourceEventSeqs);
    }
}
