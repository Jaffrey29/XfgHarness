package cn.xiaofuge.deepseek.harness.cases.session.restore;

import cn.xiaofuge.deepseek.harness.cases.session.ISessionRestoreCase;
import cn.xiaofuge.deepseek.harness.cases.session.SessionRestoreCommand;
import cn.xiaofuge.deepseek.harness.cases.session.SessionRestoreResult;
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

    /**
     * 处理Session Restore Case Impl相关逻辑。
     */
    public SessionRestoreCaseImpl(SessionRebuilderService sessionRebuilderService) {
        this.sessionRebuilderService = sessionRebuilderService;
    }

    @Override
    public SessionRestoreResult execute(SessionRestoreCommand command) {
        var snapshot = sessionRebuilderService.rebuild(command.sessionId());
        List<Map<String, Object>> messages = new ArrayList<>();
        for (var msg : snapshot.messages()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("role", msg.role().toString().toLowerCase());
            m.put("content", msg.content().toString());
            messages.add(m);
        }
        return new SessionRestoreResult(
                command.sessionId(),
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
