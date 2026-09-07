package cn.xiaofuge.deepseek.harness.cases.task.approval.factory;

import cn.xiaofuge.deepseek.harness.cases.orchestration.CasePipeline;
import cn.xiaofuge.deepseek.harness.cases.orchestration.CasePipelineFactory;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.api.dto.ApprovalActionResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.PendingApprovalResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.task.approval.node.ApprovalCommandNode;
import cn.xiaofuge.deepseek.harness.cases.task.approval.node.PendingApprovalQueryNode;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TaskApprovalPipelineFactory
        implements CasePipelineFactory<String, NoContext, ApprovalActionResponseDTO> {

    private final ApprovalCommandNode approvalCommandNode;
    private final PendingApprovalQueryNode pendingApprovalQueryNode;

    public TaskApprovalPipelineFactory(
            ApprovalCommandNode approvalCommandNode,
            PendingApprovalQueryNode pendingApprovalQueryNode
    ) {
        this.approvalCommandNode = approvalCommandNode;
        this.pendingApprovalQueryNode = pendingApprovalQueryNode;
    }

    public CasePipeline<String, NoContext, ApprovalActionResponseDTO> approvePipeline() {
        return CasePipeline.of("Approve task", approvalCommandNode, () -> NoContext.INSTANCE);
    }

    public CasePipeline<Integer, NoContext, List<PendingApprovalResponseDTO>> pendingApprovalsPipeline() {
        return CasePipeline.of("List pending approvals", pendingApprovalQueryNode, () -> NoContext.INSTANCE);
    }

    @Override
    public CasePipeline<String, NoContext, ApprovalActionResponseDTO> pipeline() {
        return approvePipeline();
    }
}
