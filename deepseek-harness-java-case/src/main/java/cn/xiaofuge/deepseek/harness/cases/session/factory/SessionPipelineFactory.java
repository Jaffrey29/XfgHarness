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

/**
 * 会话查询与恢复 Pipeline 工厂。
 * <p>流程：按查询目标组装六条无共享上下文的 Pipeline，分别路由到最近会话、消息、会话列表、详情、事件和恢复节点。</p>
 * <p>数据示例：恢复传入 {@code sessionId=BLUE-8090} 返回 {@code success=true, turnCount=N}；
 * 消息查询传入同一 sessionId 返回 {@code role=ASSISTANT, content=已记住}。</p>
 */
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

    /**
     * 构建最近会话查询 Pipeline。
     */
    public CasePipeline<SessionListRequestDTO, NoContext, List<ConversationSessionSummaryResponseDTO>> recentSessionsPipeline() {
        return CasePipeline.of("List recent sessions", conversationRecentSessionsNode, () -> NoContext.INSTANCE);
    }

    /**
     * 构建会话消息查询 Pipeline。
     */
    public CasePipeline<String, NoContext, List<ConversationMessageResponseDTO>> messagesPipeline() {
        return CasePipeline.of("List conversation messages", conversationMessagesNode, () -> NoContext.INSTANCE);
    }

    /**
     * 构建会话列表查询 Pipeline。
     */
    public CasePipeline<SessionListRequestDTO, NoContext, List<SessionQueryResponseDTO>> sessionListPipeline() {
        return CasePipeline.of("List sessions", sessionListNode, () -> NoContext.INSTANCE);
    }

    /**
     * 构建会话详情查询 Pipeline。
     */
    public CasePipeline<SessionDetailRequestDTO, NoContext, SessionQueryResponseDTO> sessionDetailPipeline() {
        return CasePipeline.of("Session detail", sessionDetailNode, () -> NoContext.INSTANCE);
    }

    /**
     * 构建会话事件查询 Pipeline。
     */
    public CasePipeline<SessionEventQueryRequestDTO, NoContext, List<SessionEventQueryResponseDTO>> eventsPipeline() {
        return CasePipeline.of("Query session events", sessionEventQueryNode, () -> NoContext.INSTANCE);
    }

    /**
     * 构建会话恢复 Pipeline。
     */
    public CasePipeline<SessionRestoreRequestDTO, NoContext, SessionRestoreResponseDTO> restorePipeline() {
        return CasePipeline.of("Restore session", sessionRestoreNode, () -> NoContext.INSTANCE);
    }

    /**
     * 构建默认执行 Pipeline，绑定首节点和上下文。
     */
    @Override
    public CasePipeline<SessionListRequestDTO, NoContext, List<SessionQueryResponseDTO>> pipeline() {
        return sessionListPipeline();
    }
}
