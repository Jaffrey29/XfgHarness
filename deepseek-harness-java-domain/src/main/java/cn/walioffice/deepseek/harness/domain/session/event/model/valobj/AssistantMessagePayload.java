package cn.walioffice.deepseek.harness.domain.session.event.model.valobj;

import cn.walioffice.deepseek.harness.domain.llm.model.entity.Message;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.TokenUsage;

/**
 * Payload for {@code assistant/message} session events.
 * <p>
 * Carries the full {@link Message} object (role=assistant) and optional
 * {@link TokenUsage} so that {@code SessionLog.deriveMessages()} can
 * reconstruct the conversation history and token accounting.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record AssistantMessagePayload(
        long turn,
        long step,
        Message message,
        TokenUsage usage
) {}
