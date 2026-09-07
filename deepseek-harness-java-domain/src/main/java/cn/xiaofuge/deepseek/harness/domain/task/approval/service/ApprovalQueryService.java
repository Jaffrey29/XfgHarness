package cn.xiaofuge.deepseek.harness.domain.task.approval.service;

import cn.xiaofuge.deepseek.harness.domain.session.IConversationQueryService;
import cn.xiaofuge.deepseek.harness.domain.shared.model.valobj.HarnessStatusEnumVO;
import cn.xiaofuge.deepseek.harness.domain.task.IApprovalQueryService;
import cn.xiaofuge.deepseek.harness.domain.task.approval.model.valobj.PendingApprovalVO;
import java.util.List;
import org.springframework.stereotype.Service;

/** Implements approval-center projections. */
@Service
public class ApprovalQueryService implements IApprovalQueryService {

    private final IConversationQueryService conversationQueryService;

    /**
     * 处理审批 Query Service。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param conversationQueryService conversation Query Service参数。
     * @return 方法执行结果。
     */
    public ApprovalQueryService(IConversationQueryService conversationQueryService) {
        this.conversationQueryService = conversationQueryService;
    }

    @Override
    public List<PendingApprovalVO> listPendingApprovals(int limit) {
        return conversationQueryService.listRecentSessions(limit).stream()
                .filter(session -> HarnessStatusEnumVO.PENDING_APPROVAL.name().equals(session.status()))
                .map(session -> new PendingApprovalVO(
                        session.sessionId(),
                        session.title(),
                        session.preview(),
                        session.status(),
                        session.createdAt()
                ))
                .toList();
    }
}
