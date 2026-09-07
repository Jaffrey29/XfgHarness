package cn.xiaofuge.deepseek.harness.cases.task;

import cn.xiaofuge.deepseek.harness.api.dto.PendingApprovalResponseDTO;

import cn.xiaofuge.deepseek.harness.cases.task.approval.factory.TaskApprovalPipelineFactory;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PendingApprovalQueryCaseImpl implements IPendingApprovalQueryCase {
    private final TaskApprovalPipelineFactory taskApprovalPipelineFactory;

    public PendingApprovalQueryCaseImpl(TaskApprovalPipelineFactory taskApprovalPipelineFactory) {
        this.taskApprovalPipelineFactory = taskApprovalPipelineFactory;
    }

    @Override
    public List<PendingApprovalResponseDTO> listPendingApprovals(int limit) {
        return taskApprovalPipelineFactory.pendingApprovalsPipeline().execute(limit);
    }
}
