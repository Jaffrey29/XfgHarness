package cn.xiaofuge.deepseek.harness.cases.task.approval.node;

import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.cases.support.CaseGuard;
import cn.xiaofuge.deepseek.harness.api.dto.ApprovalActionResponseDTO;
import cn.xiaofuge.deepseek.harness.domain.task.IApprovalCommandService;
import org.springframework.stereotype.Service;

@Service
public class ApprovalCommandNode extends AbstractStrategyRouter<String, NoContext, ApprovalActionResponseDTO> {

    private final IApprovalCommandService approvalCommandService;

    public ApprovalCommandNode(IApprovalCommandService approvalCommandService) {
        this.approvalCommandService = approvalCommandService;
    }

    @Override
    protected ApprovalActionResponseDTO doApply(String sessionId, NoContext context) {
        CaseGuard.requireSessionId(sessionId);
        var result = approvalCommandService.approve(sessionId);
        return new ApprovalActionResponseDTO(
                result.sessionId(), result.status(), result.message(), result.operatedAt()
        );
    }

    @Override
    public StrategyHandler<String, NoContext, ApprovalActionResponseDTO> getNext(String sessionId, NoContext context) {
        return defaultStrategyHandler;
    }
}
