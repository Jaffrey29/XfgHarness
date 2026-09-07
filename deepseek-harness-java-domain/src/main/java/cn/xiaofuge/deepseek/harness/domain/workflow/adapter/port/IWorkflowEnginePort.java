package cn.xiaofuge.deepseek.harness.domain.workflow.adapter.port;

import cn.xiaofuge.deepseek.harness.domain.workflow.model.entity.WorkflowRun;
import cn.xiaofuge.deepseek.harness.domain.workflow.model.entity.WorkflowStartRequest;

/**
 * Port for executing workflow orchestration scripts.
 *
 * <p>调用流程：定义工作流 → 启动 → 节点推进 → 事件/状态回写。</p>
 * <p>示例：工作流按节点顺序执行，每个节点可写回下一步上下文。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IWorkflowEnginePort {

    /**
     * Start a workflow run.
     *
     * @param request start request
     * @return live workflow run handle
     */
    WorkflowRun start(WorkflowStartRequest request);
}
