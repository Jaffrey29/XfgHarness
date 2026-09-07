package cn.xiaofuge.deepseek.harness.cases.task;

import cn.xiaofuge.deepseek.harness.api.dto.ApprovalActionResponseDTO;

import cn.xiaofuge.deepseek.harness.cases.task.approval.factory.TaskApprovalPipelineFactory;
import org.springframework.stereotype.Service;

@Service
public class ApprovalCommandCaseImpl implements IApprovalCommandCase {
    private final TaskApprovalPipelineFactory taskApprovalPipelineFactory;

    public ApprovalCommandCaseImpl(TaskApprovalPipelineFactory taskApprovalPipelineFactory) {
        this.taskApprovalPipelineFactory = taskApprovalPipelineFactory;
    }

    @Override
    public ApprovalActionResponseDTO approve(String sessionId) {
        return taskApprovalPipelineFactory.approvePipeline().execute(sessionId);
    }
}
