package cn.walioffice.deepseek.harness.domain.task;

import cn.walioffice.deepseek.harness.domain.task.execution.model.valobj.RuntimeExecutionPlanVO;
import cn.walioffice.deepseek.harness.domain.task.queue.model.valobj.QueuedTaskResultVO;

/**
 * Owns queued-task lifecycle behavior.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IHarnessTaskQueueService {

    QueuedTaskResultVO createQueuedTask(String sessionId, RuntimeExecutionPlanVO plan);
}
