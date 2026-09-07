package cn.xiaofuge.deepseek.harness.cases.session.node;

import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.api.dto.ConversationMessageResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.support.CaseGuard;
import cn.xiaofuge.deepseek.harness.domain.session.IConversationQueryService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ConversationMessagesNode
        extends AbstractStrategyRouter<String, NoContext, List<ConversationMessageResponseDTO>> {

    private final IConversationQueryService conversationQueryService;

    public ConversationMessagesNode(IConversationQueryService conversationQueryService) {
        this.conversationQueryService = conversationQueryService;
    }

    @Override
    protected List<ConversationMessageResponseDTO> doApply(String sessionId, NoContext context) {
        CaseGuard.requireSessionId(sessionId);
        return conversationQueryService.listMessages(sessionId).stream()
                .map(message -> new ConversationMessageResponseDTO(
                        message.role(), message.content(), message.occurredAt()
                ))
                .toList();
    }

    @Override
    public StrategyHandler<String, NoContext, List<ConversationMessageResponseDTO>> getNext(
            String sessionId, NoContext context) {
        return defaultStrategyHandler;
    }
}
