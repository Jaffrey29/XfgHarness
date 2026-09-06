package cn.walioffice.deepseek.harness.api.response.conversation;

import java.time.Instant;

/**
 * 返回 UI 可见的一条对话消息。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record ConversationMessageResponse(
        String role,
        String content,
        Instant occurredAt
) {
}
