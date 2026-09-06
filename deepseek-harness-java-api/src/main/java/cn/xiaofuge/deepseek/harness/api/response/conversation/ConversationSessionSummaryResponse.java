package cn.xiaofuge.deepseek.harness.api.response.conversation;

import java.time.Instant;

/**
 * 返回 UI 使用的会话摘要。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record ConversationSessionSummaryResponse(
        String sessionId,
        String title,
        String preview,
        String status,
        Instant createdAt,
        String agentId
) {
    /**
     * 兼容旧调用方的五参构造。
     */
    public ConversationSessionSummaryResponse(String sessionId, String title, String preview,
                                              String status, Instant createdAt) {
        this(sessionId, title, preview, status, createdAt, null);
    }
}
