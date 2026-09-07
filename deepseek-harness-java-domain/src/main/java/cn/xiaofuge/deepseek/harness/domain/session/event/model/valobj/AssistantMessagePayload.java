package cn.xiaofuge.deepseek.harness.domain.session.event.model.valobj;

import cn.xiaofuge.deepseek.harness.domain.model.entity.Message;
import cn.xiaofuge.deepseek.harness.domain.llm.model.valobj.TokenUsage;

/**
 * AssistantMessagePayload 表示AssistantMessagePayload消息。
 */
public record AssistantMessagePayload(
        long turn,
        long step,
        Message message,
        TokenUsage usage
) {}
