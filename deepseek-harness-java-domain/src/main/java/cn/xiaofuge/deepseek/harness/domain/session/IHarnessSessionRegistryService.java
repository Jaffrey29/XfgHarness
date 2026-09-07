package cn.xiaofuge.deepseek.harness.domain.session;

import cn.xiaofuge.deepseek.harness.domain.task.execution.model.valobj.RuntimeExecutionPlanVO;
import cn.xiaofuge.deepseek.harness.domain.session.registry.model.valobj.SessionOpenedResultVO;

/**
 * Owns session aggregate lifecycle behavior.
 *
 * <p>调用流程：事件写入 SessionEventLog → 事件溯源重建会话 → 查询或恢复。</p>
 * <p>示例：追加用户消息、模型消息和工具结果事件后，可按序号回放完整对话。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IHarnessSessionRegistryService {

    SessionOpenedResultVO openQueuedSession(RuntimeExecutionPlanVO plan);
}
