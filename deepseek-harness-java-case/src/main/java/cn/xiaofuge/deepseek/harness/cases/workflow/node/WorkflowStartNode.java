package cn.xiaofuge.deepseek.harness.cases.workflow.node;

import cn.xiaofuge.deepseek.harness.api.dto.StartWorkflowRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.WorkflowEventResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.WorkflowStartResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.cases.support.CaseGuard;
import cn.xiaofuge.deepseek.harness.domain.workflow.IWorkflowService;
import cn.xiaofuge.deepseek.harness.domain.workflow.model.entity.WorkflowRun;
import cn.xiaofuge.deepseek.harness.domain.workflow.service.WorkflowEventPolicy;
import cn.xiaofuge.deepseek.harness.domain.workflow.service.WorkflowStartPolicy;
import org.springframework.stereotype.Service;

@Service
public class WorkflowStartNode
        extends AbstractStrategyRouter<WorkflowStartNode.Input, NoContext, WorkflowStartResponseDTO> {

    private final IWorkflowService workflowService;

    public WorkflowStartNode(IWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @Override
    protected WorkflowStartResponseDTO doApply(Input input, NoContext context) {
        CaseGuard.requireNonNull(input.request(), "workflow request must not be null");
        WorkflowRun run = workflowService.start(WorkflowStartPolicy.adhoc(
                input.request().script(),
                input.request().name(),
                input.request().args(),
                input.request().parentCwd()
        ));
        if (input.eventSink() != null) {
            run.result().whenComplete((result, error) -> {
                var policy = WorkflowEventPolicy.completed(run.id(), result, error);
                input.eventSink().accept(new WorkflowEventResponseDTO(
                        policy.runId(),
                        policy.kind().name().toLowerCase(),
                        policy.status(),
                        policy.output(),
                        policy.error()
                ));
            });
        }
        return new WorkflowStartResponseDTO(run.id(), "STARTED");
    }

    @Override
    public StrategyHandler<Input, NoContext, WorkflowStartResponseDTO> getNext(Input input, NoContext context) {
        return defaultStrategyHandler;
    }

    public record Input(
            StartWorkflowRequestDTO request,
            java.util.function.Consumer<WorkflowEventResponseDTO> eventSink
    ) {
    }
}
