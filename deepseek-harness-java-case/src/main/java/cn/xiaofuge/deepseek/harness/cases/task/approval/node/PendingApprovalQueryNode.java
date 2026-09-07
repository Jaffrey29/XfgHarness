package cn.xiaofuge.deepseek.harness.cases.task.approval.node;

import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.api.dto.PendingApprovalResponseDTO;
import cn.xiaofuge.deepseek.harness.domain.task.IApprovalQueryService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PendingApprovalQueryNode
        extends AbstractStrategyRouter<Integer, NoContext, List<PendingApprovalResponseDTO>> {

    private final IApprovalQueryService approvalQueryService;

    public PendingApprovalQueryNode(IApprovalQueryService approvalQueryService) {
        this.approvalQueryService = approvalQueryService;
    }

    @Override
    protected List<PendingApprovalResponseDTO> doApply(Integer limit, NoContext context) {
        return approvalQueryService.listPendingApprovals(limit).stream()
                .map(item -> new PendingApprovalResponseDTO(
                        item.sessionId(), item.title(), item.preview(), item.status(), item.createdAt()
                ))
                .toList();
    }

    @Override
    public StrategyHandler<Integer, NoContext, List<PendingApprovalResponseDTO>> getNext(Integer limit, NoContext context) {
        return defaultStrategyHandler;
    }
}
