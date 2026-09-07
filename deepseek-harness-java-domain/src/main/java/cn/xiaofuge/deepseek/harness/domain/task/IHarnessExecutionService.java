package cn.xiaofuge.deepseek.harness.domain.task;

import cn.xiaofuge.deepseek.harness.domain.task.execution.model.valobj.HarnessExecutionResultVO;

/**
 * Owns runtime execution for accepted sessions.
 *
 * <p>调用流程：提交 → 权限评估 → 审批 → 队列 → 执行 → 状态回写。</p>
 * <p>示例：需要审批的命令先挂起，批准后进入执行队列。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IHarnessExecutionService {

    /**
     * Executes the latest user prompt stored in one accepted session.
     *
     * @param sessionId session identifier
     * @return final execution result
     */
    HarnessExecutionResultVO executeSession(String sessionId);
}
