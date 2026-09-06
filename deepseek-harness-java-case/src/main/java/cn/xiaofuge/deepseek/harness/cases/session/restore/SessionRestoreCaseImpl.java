package cn.xiaofuge.deepseek.harness.cases.session.restore;

import cn.xiaofuge.deepseek.harness.api.dto.session.SessionRestoreRequest;
import cn.xiaofuge.deepseek.harness.api.dto.session.SessionRestoreResponse;
import cn.xiaofuge.deepseek.harness.domain.session.event.service.SessionRebuilderService;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 薄委托到 {@link SessionRebuilderService}。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class SessionRestoreCaseImpl implements ISessionRestoreCase {

    private final SessionRebuilderService sessionRebuilderService;

    public SessionRestoreCaseImpl(SessionRebuilderService sessionRebuilderService) {
        this.sessionRebuilderService = sessionRebuilderService;
    }

    @Override
    public SessionRestoreResponse execute(SessionRestoreRequest request) {
        var snapshot = sessionRebuilderService.rebuild(request.sessionId());
        List<Map<String, Object>> messages = new ArrayList<>();
        for (var msg : snapshot.messages()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("role", msg.role().toString().toLowerCase());
            m.put("content", msg.content().toString());
            messages.add(m);
        }
        return new SessionRestoreResponse(
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
}
