package cn.xiaofuge.deepseek.harness.cases.session.node;

import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.api.dto.ConversationSessionSummaryResponseDTO;
import cn.xiaofuge.deepseek.harness.domain.session.IConversationQueryService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ConversationRecentSessionsNode
        extends AbstractStrategyRouter<Integer, NoContext, List<ConversationSessionSummaryResponseDTO>> {

    private final IConversationQueryService conversationQueryService;

    public ConversationRecentSessionsNode(IConversationQueryService conversationQueryService) {
        this.conversationQueryService = conversationQueryService;
    }

    @Override
    protected List<ConversationSessionSummaryResponseDTO> doApply(Integer limit, NoContext context) {
        return conversationQueryService.listRecentSessions(limit).stream()
                .map(summary -> new ConversationSessionSummaryResponseDTO(
                        summary.sessionId(), summary.title(), summary.preview(), summary.status(),
                        summary.createdAt(), summary.agentId()
                ))
                .toList();
    }

    @Override
    public StrategyHandler<Integer, NoContext, List<ConversationSessionSummaryResponseDTO>> getNext(
            Integer limit, NoContext context) {
        return defaultStrategyHandler;
    }
}
