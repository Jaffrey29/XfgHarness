package cn.xiaofuge.deepseek.harness.cases.session.factory;

import cn.xiaofuge.deepseek.harness.cases.orchestration.CasePipeline;
import cn.xiaofuge.deepseek.harness.cases.orchestration.CasePipelineFactory;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.api.dto.SessionDetailRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.SessionEventQueryRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.SessionEventQueryResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.SessionListRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.SessionQueryResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.SessionRestoreRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.SessionRestoreResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.ConversationMessageResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.ConversationSessionSummaryResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.session.node.ConversationMessagesNode;
import cn.xiaofuge.deepseek.harness.cases.session.node.ConversationRecentSessionsNode;
import cn.xiaofuge.deepseek.harness.cases.session.node.SessionDetailNode;
import cn.xiaofuge.deepseek.harness.cases.session.node.SessionEventQueryNode;
import cn.xiaofuge.deepseek.harness.cases.session.node.SessionListNode;
import cn.xiaofuge.deepseek.harness.cases.session.node.SessionRestoreNode;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SessionPipelineFactory
        implements CasePipelineFactory<SessionListRequestDTO, NoContext, List<SessionQueryResponseDTO>> {

    private final ConversationRecentSessionsNode conversationRecentSessionsNode;
    private final ConversationMessagesNode conversationMessagesNode;
    private final SessionListNode sessionListNode;
    private final SessionDetailNode sessionDetailNode;
    private final SessionEventQueryNode sessionEventQueryNode;
    private final SessionRestoreNode sessionRestoreNode;

    public SessionPipelineFactory(
            ConversationRecentSessionsNode conversationRecentSessionsNode,
            ConversationMessagesNode conversationMessagesNode,
            SessionListNode sessionListNode,
            SessionDetailNode sessionDetailNode,
            SessionEventQueryNode sessionEventQueryNode,
            SessionRestoreNode sessionRestoreNode
    ) {
        this.conversationRecentSessionsNode = conversationRecentSessionsNode;
        this.conversationMessagesNode = conversationMessagesNode;
        this.sessionListNode = sessionListNode;
        this.sessionDetailNode = sessionDetailNode;
        this.sessionEventQueryNode = sessionEventQueryNode;
        this.sessionRestoreNode = sessionRestoreNode;
    }

    public CasePipeline<Integer, NoContext, List<ConversationSessionSummaryResponseDTO>> recentSessionsPipeline() {
        return CasePipeline.of("List recent sessions", conversationRecentSessionsNode, () -> NoContext.INSTANCE);
    }

    public CasePipeline<String, NoContext, List<ConversationMessageResponseDTO>> messagesPipeline() {
        return CasePipeline.of("List conversation messages", conversationMessagesNode, () -> NoContext.INSTANCE);
    }

    public CasePipeline<SessionListRequestDTO, NoContext, List<SessionQueryResponseDTO>> sessionListPipeline() {
        return CasePipeline.of("List sessions", sessionListNode, () -> NoContext.INSTANCE);
    }

    public CasePipeline<SessionDetailRequestDTO, NoContext, SessionQueryResponseDTO> sessionDetailPipeline() {
        return CasePipeline.of("Session detail", sessionDetailNode, () -> NoContext.INSTANCE);
    }

    public CasePipeline<SessionEventQueryRequestDTO, NoContext, List<SessionEventQueryResponseDTO>> eventsPipeline() {
        return CasePipeline.of("Query session events", sessionEventQueryNode, () -> NoContext.INSTANCE);
    }

    public CasePipeline<SessionRestoreRequestDTO, NoContext, SessionRestoreResponseDTO> restorePipeline() {
        return CasePipeline.of("Restore session", sessionRestoreNode, () -> NoContext.INSTANCE);
    }

    @Override
    public CasePipeline<SessionListRequestDTO, NoContext, List<SessionQueryResponseDTO>> pipeline() {
        return sessionListPipeline();
    }
}
