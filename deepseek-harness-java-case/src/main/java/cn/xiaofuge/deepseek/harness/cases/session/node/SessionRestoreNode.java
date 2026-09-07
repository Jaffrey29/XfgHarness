package cn.xiaofuge.deepseek.harness.cases.session.node;

import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.api.dto.SessionRestoreRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.SessionRestoreResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.support.CaseGuard;
import cn.xiaofuge.deepseek.harness.domain.session.event.service.SessionRebuilderService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SessionRestoreNode
        extends AbstractStrategyRouter<SessionRestoreRequestDTO, NoContext, SessionRestoreResponseDTO> {

    private final SessionRebuilderService sessionRebuilderService;

    public SessionRestoreNode(SessionRebuilderService sessionRebuilderService) {
        this.sessionRebuilderService = sessionRebuilderService;
    }

    @Override
    protected SessionRestoreResponseDTO doApply(SessionRestoreRequestDTO request, NoContext context) {
        CaseGuard.requireNonNull(request, "session restore request must not be null");
        CaseGuard.requireSessionId(request.sessionId());
        var snapshot = sessionRebuilderService.rebuild(request.sessionId());
        List<Map<String, Object>> messages = new ArrayList<>();
        for (var message : snapshot.messages()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("role", message.role().toString().toLowerCase());
            item.put("content", message.content().toString());
            messages.add(item);
        }
        return new SessionRestoreResponseDTO(
                request.sessionId(),
                true,
                snapshot.phase() != null ? snapshot.phase().getClass().getSimpleName().toUpperCase() : "IDLE",
                snapshot.turnCount(),
                snapshot.stepCount(),
                snapshot.lastTurnEndReason() != null ? snapshot.lastTurnEndReason().toString() : null,
                messages,
                Instant.now()
        );
    }

    @Override
    public StrategyHandler<SessionRestoreRequestDTO, NoContext, SessionRestoreResponseDTO> getNext(
            SessionRestoreRequestDTO request, NoContext context) {
        return defaultStrategyHandler;
    }
}
