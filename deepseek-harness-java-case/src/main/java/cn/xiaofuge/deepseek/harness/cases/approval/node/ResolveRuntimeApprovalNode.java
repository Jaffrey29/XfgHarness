package cn.xiaofuge.deepseek.harness.cases.approval.node;

import cn.xiaofuge.deepseek.harness.api.dto.ResolveRuntimeApprovalResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.approval.factory.RuntimeApprovalResolveInput;
import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.cases.support.CaseGuard;
import cn.xiaofuge.deepseek.harness.domain.agent.adapter.port.IRuntimeApprovalBroker;
import cn.xiaofuge.deepseek.harness.domain.agent.model.valobj.RuntimeApprovalDecisionVO;
import cn.xiaofuge.deepseek.harness.domain.tool.service.RuntimeApprovalGate.ApprovalVerdict;
import org.springframework.stereotype.Service;

@Service
public class ResolveRuntimeApprovalNode
        extends AbstractStrategyRouter<RuntimeApprovalResolveInput, NoContext, ResolveRuntimeApprovalResponseDTO> {

    private final IRuntimeApprovalBroker approvalBroker;

    public ResolveRuntimeApprovalNode(IRuntimeApprovalBroker approvalBroker) {
        this.approvalBroker = approvalBroker;
    }

    @Override
    protected ResolveRuntimeApprovalResponseDTO doApply(
            RuntimeApprovalResolveInput input, NoContext context
    ) {
        CaseGuard.requireNonNull(input, "approval input must not be null");
        CaseGuard.requireText(input.approvalId(), "approvalId must not be blank");
        boolean resolved = approvalBroker.resolve(input.approvalId(), toVerdict(input.decision()));
        return resolved
                ? new ResolveRuntimeApprovalResponseDTO(true, input.decision().name(), null)
                : new ResolveRuntimeApprovalResponseDTO(false, null, "approval not found or already resolved");
    }

    @Override
    public StrategyHandler<RuntimeApprovalResolveInput, NoContext, ResolveRuntimeApprovalResponseDTO> getNext(
            RuntimeApprovalResolveInput input, NoContext context
    ) {
        return defaultStrategyHandler;
    }

    private ApprovalVerdict toVerdict(RuntimeApprovalDecisionVO decision) {
        return switch (decision) {
            case ALLOW_ONCE -> ApprovalVerdict.ALLOW_ONCE;
            case ALLOW_SESSION -> ApprovalVerdict.ALLOW_SESSION;
            case DENY -> ApprovalVerdict.DENY;
            case CANCEL -> ApprovalVerdict.CANCEL;
        };
    }
}
