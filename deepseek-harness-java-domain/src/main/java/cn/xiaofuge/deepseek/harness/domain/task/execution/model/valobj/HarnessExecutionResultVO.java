package cn.xiaofuge.deepseek.harness.domain.task.execution.model.valobj;

import cn.xiaofuge.deepseek.harness.domain.shared.model.valobj.HarnessStatusEnumVO;
import java.time.Instant;

/**
 * HarnessExecutionResultVO 表示Harnes列表HarnessExecutionResult结果的领域值对象。
 */
public record HarnessExecutionResultVO(
        String sessionId,
        HarnessStatusEnumVO status,
        String assistantMessage,
        Instant operatedAt
) {
}
