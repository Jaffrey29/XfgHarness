package cn.xiaofuge.deepseek.harness.domain.session.event.model.valobj;

import cn.xiaofuge.deepseek.harness.domain.model.entity.Message;
import java.util.Map;

/**
 * ToolResultPayload 表示ToolResultPayload结果。
 */
public record ToolResultPayload(
        long turn,
        long step,
        Message message,
        Map<String, Object> error,
        Map<String, Object> meta
) {}
