package cn.xiaofuge.deepseek.harness.cases.session;

import cn.xiaofuge.deepseek.harness.api.dto.ConversationMessageResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.ConversationSessionSummaryResponseDTO;

import cn.xiaofuge.deepseek.harness.cases.session.factory.SessionPipelineFactory;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ConversationQueryCaseImpl implements IConversationQueryCase {
    private final SessionPipelineFactory sessionPipelineFactory;

    public ConversationQueryCaseImpl(SessionPipelineFactory sessionPipelineFactory) {
        this.sessionPipelineFactory = sessionPipelineFactory;
    }

    @Override
    public List<ConversationSessionSummaryResponseDTO> listRecentSessions(int limit) {
        return sessionPipelineFactory.recentSessionsPipeline().execute(limit);
    }

    @Override
    public List<ConversationMessageResponseDTO> listMessages(String sessionId) {
        return sessionPipelineFactory.messagesPipeline().execute(sessionId);
    }
}
