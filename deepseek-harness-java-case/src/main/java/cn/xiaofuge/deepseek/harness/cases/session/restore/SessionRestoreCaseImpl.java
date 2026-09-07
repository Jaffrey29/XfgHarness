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
 * <p>调用流程：Controller/API → 会话用例 → SessionQueryService/SessionRebuilderService → SessionEventStore。</p>
 * <p>示例：按 sessionId 查询事件后，可重建完整消息时间线。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class SessionRestoreCaseImpl implements ISessionRestoreCase {

    private final SessionRebuilderService sessionRebuilderService;

    /**
     * 处理会话 Restore Case Impl。
     * 流程：读取命令 → 执行用例编排 → 返回结果。
     * @param sessionRebuilderService 会话 Rebuilder Service参数。
     * @return 方法执行结果。
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
