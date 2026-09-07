package cn.xiaofuge.deepseek.harness.domain.workflow.model.valobj;

import cn.xiaofuge.deepseek.harness.domain.workflow.service.WorkflowEventPolicy;

public record WorkflowEventVO(
        String runId,
        WorkflowEventKindVO kind,
        String status,
        String output,
        String error
) {
    public static WorkflowEventVO fromPolicy(WorkflowEventPolicy policy) {
        return new WorkflowEventVO(
                policy.runId(),
                policy.kind() == WorkflowEventPolicy.Kind.ERROR
                        ? WorkflowEventKindVO.ERROR
                        : WorkflowEventKindVO.DONE,
                policy.status(),
                policy.output(),
                policy.error()
        );
    }
}
