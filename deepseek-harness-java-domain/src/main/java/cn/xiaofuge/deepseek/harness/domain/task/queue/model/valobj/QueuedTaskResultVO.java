package cn.xiaofuge.deepseek.harness.domain.task.queue.model.valobj;

import cn.xiaofuge.deepseek.harness.domain.shared.model.valobj.HarnessStatusEnumVO;
import java.time.Instant;
import java.util.List;

/**
 * 排队任务结果对象。
 * <p>
 * 执行流程：
 * 1. 领域层完成任务入队；
 * 2. 组装任务编号、会话编号和状态；
 * 3. 返回给上层作为提交结果。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record QueuedTaskResultVO(
        String taskId,
        String sessionId,
        HarnessStatusEnumVO status,
        List<String> enabledTools,
        Instant acceptedAt
) {
}
