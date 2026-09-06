package cn.walioffice.deepseek.harness.domain.task.approval.service;

import cn.walioffice.deepseek.harness.domain.session.IConversationQueryService;
import cn.walioffice.deepseek.harness.domain.shared.model.valobj.HarnessStatusEnumVO;
import cn.walioffice.deepseek.harness.domain.task.IApprovalQueryService;
import cn.walioffice.deepseek.harness.domain.task.approval.model.valobj.PendingApprovalVO;
import java.util.List;

/** Implements approval-center projections. */
public class ApprovalQueryService implements IApprovalQueryService {

    private final IConversationQueryService conversationQueryService;

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
