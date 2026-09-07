package cn.xiaofuge.deepseek.harness.domain.workflow.service;

import cn.xiaofuge.deepseek.harness.domain.workflow.model.valobj.WorkflowResult;

/**
 * 工作流运行事件策略。
 * <p>统一运行结果到外部事件的领域语义：状态、输出和错误信息。</p>
 */
public record WorkflowEventPolicy(
        String runId,
        Kind kind,
        String status,
        String output,
        String error
) {
    public enum Kind {
        DONE,
        ERROR
    }

    public static WorkflowEventPolicy completed(String runId, WorkflowResult result, Throwable error) {
        if (error != null) {
            return new WorkflowEventPolicy(runId, Kind.ERROR, "ERROR", "", error.getMessage());
        }
        return new WorkflowEventPolicy(
                runId,
                Kind.DONE,
                result.stopReason() == null ? "COMPLETED" : result.stopReason().name(),
                result.value() == null ? "" : result.value().toString(),
                result.error().orElse(null)
        );
    }
}
