package cn.xiaofuge.deepseek.harness.cases.workflow.node;

import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.cases.support.CaseGuard;
import cn.xiaofuge.deepseek.harness.cases.workflow.WorkflowCaseImpl;
import cn.xiaofuge.deepseek.harness.domain.workflow.IWorkflowService;
import org.springframework.stereotype.Service;

@Service
public class WorkflowCancelNode
        extends AbstractStrategyRouter<WorkflowCaseImpl.WorkflowCancelInput, NoContext, Void> {

    private final IWorkflowService workflowService;

    public WorkflowCancelNode(IWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @Override
    protected Void doApply(WorkflowCaseImpl.WorkflowCancelInput input, NoContext context) {
        CaseGuard.requireNonNull(input, "workflow cancel input must not be null");
        CaseGuard.requireText(input.runId(), "runId must not be blank");
        workflowService.cancel(input.runId(), input.reason());
        return null;
    }

    @Override
    public StrategyHandler<WorkflowCaseImpl.WorkflowCancelInput, NoContext, Void> getNext(
            WorkflowCaseImpl.WorkflowCancelInput input, NoContext context
    ) {
        return defaultStrategyHandler;
    }
}
