package cn.walioffice.deepseek.harness.domain.session.event.model.valobj;

import cn.walioffice.deepseek.harness.domain.llm.model.entity.Message;
import java.util.Map;

/**
 * Payload for the {@code tool/result} session event.
 * Mirrors TS {@code tool/result} event data.
 *
 * @param turn         current turn number
 * @param step         current step number
 * @param message      the tool-result message (role=user, content=[ToolResultBlock])
 * @param error        structured error info (name+code), or null if success
 * @param meta         tool-private presentation metadata, or null
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record ToolResultPayload(
        long turn,
        long step,
        Message message,
        Map<String, Object> error,
        Map<String, Object> meta
) {}
