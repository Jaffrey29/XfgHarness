package cn.walioffice.deepseek.harness.domain.session.event.model.valobj;

import cn.walioffice.deepseek.harness.domain.llm.model.entity.Message;

/**
 * Payload for {@code user/message} session events.
 * <p>
 * Carries the full {@link Message} object (role=user) so that
 * {@code SessionLog.deriveMessages()} can reconstruct the conversation
 * history without lossy string conversions.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record UserMessagePayload(long turn, long step, Message message) {
    public UserMessagePayload {
        if (message != null && !"user".equals(message.role())) {
            throw new IllegalArgumentException("UserMessagePayload requires role=user, got: " + message.role());
        }
    }
}
