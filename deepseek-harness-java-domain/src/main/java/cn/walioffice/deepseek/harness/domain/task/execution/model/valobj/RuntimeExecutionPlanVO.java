package cn.walioffice.deepseek.harness.domain.task.execution.model.valobj;

import cn.walioffice.deepseek.harness.domain.shared.model.valobj.HarnessStatusEnumVO;
import java.time.Instant;
import java.util.List;

/**
 * Describes the resolved execution plan for a submitted task.
 *
 * @param workspaceId caller workspace identifier
 * @param prompt user prompt
 * @param profileCode execution profile code
 * @param executionMode resolved execution mode
 * @param resolvedModel resolved model identifier
 * @param enabledTools enabled tools after policy resolution
 * @param acceptedAt acceptance timestamp
 * @param status initial task/session status
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record RuntimeExecutionPlanVO(
        String workspaceId,
        String prompt,
        String profileCode,
        String executionMode,
        String resolvedModel,
        List<String> enabledTools,
        Instant acceptedAt,
        HarnessStatusEnumVO status
) {
}
