package cn.xiaofuge.deepseek.harness.cases.session.node;

import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.api.dto.SessionDetailRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.SessionQueryResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.support.CaseGuard;
import cn.xiaofuge.deepseek.harness.domain.session.IConversationQueryService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SessionDetailNode
        extends AbstractStrategyRouter<SessionDetailRequestDTO, NoContext, SessionQueryResponseDTO> {

    private final IConversationQueryService conversationQueryService;

    public SessionDetailNode(IConversationQueryService conversationQueryService) {
        this.conversationQueryService = conversationQueryService;
    }

    @Override
    protected SessionQueryResponseDTO doApply(SessionDetailRequestDTO query, NoContext context) {
        CaseGuard.requireSessionId(query.sessionId());
        var summary = conversationQueryService.querySessionDetail(query.sessionId());
        List<Map<String, Object>> messages = new ArrayList<>();
        if (summary.messages() != null) {
            for (var message : summary.messages()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("role", message.role());
                item.put("content", message.content());
                messages.add(item);
            }
        }
        return new SessionQueryResponseDTO(
                summary.sessionId(), summary.workspaceId(), summary.status(), summary.modelCode(),
                summary.profileCode(), summary.createdAt(), summary.lastActivityAt(), messages
        );
    }

    @Override
    public StrategyHandler<SessionDetailRequestDTO, NoContext, SessionQueryResponseDTO> getNext(
            SessionDetailRequestDTO query, NoContext context) {
        return defaultStrategyHandler;
    }
}
