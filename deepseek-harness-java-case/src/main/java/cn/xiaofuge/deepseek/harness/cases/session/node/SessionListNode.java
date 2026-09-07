package cn.xiaofuge.deepseek.harness.cases.session.node;

import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.api.dto.SessionListRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.SessionQueryResponseDTO;
import cn.xiaofuge.deepseek.harness.domain.session.IConversationQueryService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SessionListNode
        extends AbstractStrategyRouter<SessionListRequestDTO, NoContext, List<SessionQueryResponseDTO>> {

    private final IConversationQueryService conversationQueryService;

    public SessionListNode(IConversationQueryService conversationQueryService) {
        this.conversationQueryService = conversationQueryService;
    }

    @Override
    protected List<SessionQueryResponseDTO> doApply(SessionListRequestDTO query, NoContext context) {
        return conversationQueryService.querySessions(query.workspaceId(), query.offset(), query.limit())
                .stream()
                .map(s -> new SessionQueryResponseDTO(
                        s.sessionId(), s.workspaceId(), s.status(), s.modelCode(), s.profileCode(),
                        s.createdAt(), s.lastActivityAt(), null
                ))
                .toList();
    }

    @Override
    public StrategyHandler<SessionListRequestDTO, NoContext, List<SessionQueryResponseDTO>> getNext(
            SessionListRequestDTO query, NoContext context) {
        return defaultStrategyHandler;
    }
}
