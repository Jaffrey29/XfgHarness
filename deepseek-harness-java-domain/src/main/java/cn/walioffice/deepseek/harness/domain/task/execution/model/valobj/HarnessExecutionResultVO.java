package cn.walioffice.deepseek.harness.domain.task.execution.model.valobj;

import cn.walioffice.deepseek.harness.domain.shared.model.valobj.HarnessStatusEnumVO;
import java.time.Instant;

/**
 * Returns the result of executing one accepted session.
 *
 * @param sessionId session identifier
 * @param status final execution status
 * @param assistantMessage assistant-visible reply or failure text
 * @param operatedAt execution completion time
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record HarnessExecutionResultVO(
        String sessionId,
        HarnessStatusEnumVO status,
        String assistantMessage,
        Instant operatedAt
) {
}
