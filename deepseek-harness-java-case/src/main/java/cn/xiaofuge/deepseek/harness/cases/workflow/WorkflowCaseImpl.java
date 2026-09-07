package cn.xiaofuge.deepseek.harness.cases.workflow;

import cn.xiaofuge.deepseek.harness.api.dto.StartWorkflowRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.WorkflowEventResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.WorkflowStartResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.workflow.factory.WorkflowPipelineFactory;
import cn.xiaofuge.deepseek.harness.cases.workflow.node.WorkflowStartNode;
import cn.xiaofuge.deepseek.harness.domain.workflow.model.valobj.WorkflowEventVO;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;

@Service
public class WorkflowCaseImpl implements IWorkflowCase {

    private final WorkflowPipelineFactory workflowPipelineFactory;

    public WorkflowCaseImpl(WorkflowPipelineFactory workflowPipelineFactory) {
        this.workflowPipelineFactory = workflowPipelineFactory;
    }

    @Override
    public WorkflowStartResponseDTO start(StartWorkflowRequestDTO request) {
        return workflowPipelineFactory.startPipeline()
                .execute(new WorkflowStartNode.Input(request, null));
    }

    @Override
    public WorkflowStartResponseDTO startStreaming(
            StartWorkflowRequestDTO request,
            Consumer<WorkflowEventResponseDTO> eventSink
    ) {
        return workflowPipelineFactory.startPipeline()
                .execute(new WorkflowStartNode.Input(request, eventSink));
    }

    @Override
    public void cancel(String runId, String reason) {
        workflowPipelineFactory.cancelPipeline().execute(new WorkflowCancelInput(runId, reason));
    }

    public record WorkflowCancelInput(String runId, String reason) {
    }
}
