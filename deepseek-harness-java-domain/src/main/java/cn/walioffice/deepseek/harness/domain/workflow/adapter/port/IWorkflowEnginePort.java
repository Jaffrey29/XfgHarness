package cn.walioffice.deepseek.harness.domain.workflow.adapter.port;

import cn.walioffice.deepseek.harness.domain.workflow.model.entity.WorkflowRun;
import cn.walioffice.deepseek.harness.domain.workflow.model.entity.WorkflowStartRequest;

/**
 * Port for executing workflow orchestration scripts.
 *
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
