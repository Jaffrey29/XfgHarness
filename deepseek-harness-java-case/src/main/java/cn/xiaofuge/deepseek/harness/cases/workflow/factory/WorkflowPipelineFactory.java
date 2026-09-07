package cn.xiaofuge.deepseek.harness.cases.workflow.factory;

import cn.xiaofuge.deepseek.harness.api.dto.WorkflowStartResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.orchestration.CasePipeline;
import cn.xiaofuge.deepseek.harness.cases.orchestration.CasePipelineFactory;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.cases.workflow.WorkflowCaseImpl;
import cn.xiaofuge.deepseek.harness.cases.workflow.node.WorkflowCancelNode;
import cn.xiaofuge.deepseek.harness.cases.workflow.node.WorkflowStartNode;
import org.springframework.stereotype.Service;

@Service
public class WorkflowPipelineFactory
        implements CasePipelineFactory<WorkflowStartNode.Input, NoContext, WorkflowStartResponseDTO> {

    private final WorkflowStartNode workflowStartNode;
    private final WorkflowCancelNode workflowCancelNode;

    public WorkflowPipelineFactory(
            WorkflowStartNode workflowStartNode,
            WorkflowCancelNode workflowCancelNode
    ) {
        this.workflowStartNode = workflowStartNode;
        this.workflowCancelNode = workflowCancelNode;
    }

    public CasePipeline<WorkflowStartNode.Input, NoContext, WorkflowStartResponseDTO> startPipeline() {
        return CasePipeline.of("Start workflow", workflowStartNode, () -> NoContext.INSTANCE);
    }

    public CasePipeline<WorkflowCaseImpl.WorkflowCancelInput, NoContext, Void> cancelPipeline() {
        return CasePipeline.of("Cancel workflow", workflowCancelNode, () -> NoContext.INSTANCE);
    }

    @Override
    public CasePipeline<WorkflowStartNode.Input, NoContext, WorkflowStartResponseDTO> pipeline() {
        return startPipeline();
    }
}
