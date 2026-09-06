package cn.xiaofuge.deepseek.harness.domain.session.conversation.model.valobj;

import java.time.Instant;

/**
 * Represents one chat-visible conversation message.
 *
 * @param role message role such as user or assistant
 * @param content message content
 * @param occurredAt message timestamp
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record ConversationMessageVO(
        String role,
        String content,
        Instant occurredAt
) {
}
