package cn.xiaofuge.deepseek.harness.cases.approval.factory;

import cn.xiaofuge.deepseek.harness.api.dto.ResolveRuntimeApprovalResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.RuntimeApprovalResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.approval.node.ListPendingApprovalsNode;
import cn.xiaofuge.deepseek.harness.cases.approval.node.ResolveRuntimeApprovalNode;
import cn.xiaofuge.deepseek.harness.cases.orchestration.CasePipeline;
import cn.xiaofuge.deepseek.harness.cases.orchestration.CasePipelineFactory;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RuntimeApprovalPipelineFactory
        implements CasePipelineFactory<RuntimeApprovalResolveInput, NoContext, ResolveRuntimeApprovalResponseDTO> {

    private final ListPendingApprovalsNode listPendingApprovalsNode;
    private final ResolveRuntimeApprovalNode resolveRuntimeApprovalNode;

    public RuntimeApprovalPipelineFactory(
            ListPendingApprovalsNode listPendingApprovalsNode,
            ResolveRuntimeApprovalNode resolveRuntimeApprovalNode
    ) {
        this.listPendingApprovalsNode = listPendingApprovalsNode;
        this.resolveRuntimeApprovalNode = resolveRuntimeApprovalNode;
    }

    public CasePipeline<Void, NoContext, List<RuntimeApprovalResponseDTO>> listPendingPipeline() {
        return CasePipeline.of("List runtime approvals", listPendingApprovalsNode, () -> NoContext.INSTANCE);
    }

    public CasePipeline<RuntimeApprovalResolveInput, NoContext, ResolveRuntimeApprovalResponseDTO> resolvePipeline() {
        return CasePipeline.of("Resolve runtime approval", resolveRuntimeApprovalNode, () -> NoContext.INSTANCE);
    }

    @Override
    public CasePipeline<RuntimeApprovalResolveInput, NoContext, ResolveRuntimeApprovalResponseDTO> pipeline() {
        return resolvePipeline();
    }
}
