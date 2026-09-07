package cn.xiaofuge.deepseek.harness.cases.approval.node;

import cn.xiaofuge.deepseek.harness.api.dto.RuntimeApprovalResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.domain.agent.adapter.port.IRuntimeApprovalBroker;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ListPendingApprovalsNode
        extends AbstractStrategyRouter<Void, NoContext, List<RuntimeApprovalResponseDTO>> {

    private final IRuntimeApprovalBroker approvalBroker;

    public ListPendingApprovalsNode(IRuntimeApprovalBroker approvalBroker) {
        this.approvalBroker = approvalBroker;
    }

    @Override
    protected List<RuntimeApprovalResponseDTO> doApply(Void request, NoContext context) {
        return approvalBroker.listPending().stream()
                .map(item -> new RuntimeApprovalResponseDTO(
                        item.approvalId(),
                        item.sessionId(),
                        item.toolName(),
                        item.displayCommand(),
                        item.arguments(),
                        item.createdAt()
                ))
                .toList();
    }

    @Override
    public StrategyHandler<Void, NoContext, List<RuntimeApprovalResponseDTO>> getNext(Void request, NoContext context) {
        return defaultStrategyHandler;
    }
}
