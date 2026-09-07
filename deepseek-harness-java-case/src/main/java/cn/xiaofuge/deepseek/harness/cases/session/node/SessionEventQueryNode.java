package cn.xiaofuge.deepseek.harness.cases.session.node;

import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.api.dto.SessionEventQueryRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.SessionEventQueryResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.support.CaseGuard;
import cn.xiaofuge.deepseek.harness.domain.session.ISessionEventLogService;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SessionEventQueryNode
        extends AbstractStrategyRouter<SessionEventQueryRequestDTO, NoContext, List<SessionEventQueryResponseDTO>> {

    private final ISessionEventLogService sessionEventLogService;

    public SessionEventQueryNode(ISessionEventLogService sessionEventLogService) {
        this.sessionEventLogService = sessionEventLogService;
    }

    @Override
    protected List<SessionEventQueryResponseDTO> doApply(SessionEventQueryRequestDTO query, NoContext context) {
        CaseGuard.requireSessionId(query.sessionId());
        List<SessionEvent> events = query.fromSeq() != null
                ? sessionEventLogService.readAfter(query.sessionId(), query.fromSeq())
                : sessionEventLogService.readAll(query.sessionId());

        List<SessionEventQueryResponseDTO> result = new ArrayList<>();
        for (SessionEvent event : events) {
            if (query.toSeq() != null && event.seq() > query.toSeq()) {
                break;
            }
            String eventType = event.getClass().getSimpleName();
            if (query.eventTypes() != null && !query.eventTypes().isEmpty()
                    && !query.eventTypes().contains(eventType)) {
                continue;
            }
            result.add(new SessionEventQueryResponseDTO(event.seq(), eventType, event.time(), toMap(event)));
        }
        return result;
    }

    @Override
    public StrategyHandler<SessionEventQueryRequestDTO, NoContext, List<SessionEventQueryResponseDTO>> getNext(
            SessionEventQueryRequestDTO query, NoContext context) {
        return defaultStrategyHandler;
    }

    private Map<String, Object> toMap(SessionEvent event) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("seq", event.seq());
        data.put("type", event.getClass().getSimpleName());
        data.put("time", event.time() != null ? event.time().toString() : null);
        return data;
    }
}
