package cn.xiaofuge.deepseek.harness.domain.session.event.service;

import cn.xiaofuge.deepseek.harness.domain.session.ISessionEventLogService;
import cn.xiaofuge.deepseek.harness.domain.session.event.adapter.repository.ISessionEventStore;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionEvent;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionHeader;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionLog;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Default implementation of {@link ISessionEventLogService}.
 * <p>
 * Maintains an in-memory {@link SessionLog} per session and mirrors
 * writes to the durable {@link ISessionEventStore}. On startup or resume,
 * the log can be restored from the store.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class SessionEventLogService implements ISessionEventLogService {

    private final ISessionEventStore store;
    private final Map<String, SessionLog> logs = new ConcurrentHashMap<>();

    /**
     * 处理Session Event Log Service相关逻辑。
     */
    public SessionEventLogService(ISessionEventStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    @Override
    public SessionHeader createSession(SessionHeader header) {
        store.saveHeader(header);
        SessionLog log = new SessionLog(header.sessionId(), header);
        logs.put(header.sessionId(), log);
        return header;
    }

    @Override
    public SessionEvent append(String sessionId, SessionEvent event) {
        SessionLog log = getOrCreateLog(sessionId);
        SessionEvent appended = log.append(event);
        store.append(sessionId, appended);
        return appended;
    }

    @Override
    public List<SessionEvent> appendBatch(String sessionId, List<SessionEvent> events) {
        SessionLog log = getOrCreateLog(sessionId);
        List<SessionEvent> appended = log.appendBatch(events);
        store.appendBatch(sessionId, appended);
        return appended;
    }

    @Override
    public List<SessionEvent> readAll(String sessionId) {
        SessionLog log = logs.get(sessionId);
        if (log != null) {
            return log.events();
        }
        return store.readAll(sessionId);
    }

    @Override
    public List<SessionEvent> readAfter(String sessionId, long afterSeq) {
        SessionLog log = logs.get(sessionId);
        if (log != null) {
            return log.eventsAfter(afterSeq);
        }
        return store.readAfter(sessionId, afterSeq);
    }

    @Override
    public SessionHeader getHeader(String sessionId) {
        return store.getHeader(sessionId)
                .orElseThrow(() -> new IllegalStateException("No session header for: " + sessionId));
    }

    @Override
    public long getLastSeq(String sessionId) {
        SessionLog log = logs.get(sessionId);
        if (log != null) {
            return log.lastSeq();
        }
        return store.getLastSeq(sessionId);
    }

    @Override
    public void restore(String sessionId) {
        SessionHeader header = getHeader(sessionId);
        List<SessionEvent> events = store.readAll(sessionId);
        SessionLog log = new SessionLog(sessionId, header);
        log.restore(events);
        logs.put(sessionId, log);
    }

    // ── Helpers ──────────────────────────────────────────────────

    /**
     * 获取Or Create Log。
     */
    private SessionLog getOrCreateLog(String sessionId) {
        return logs.computeIfAbsent(sessionId, sid -> {
            SessionHeader header = store.getHeader(sid)
                    .orElseThrow(() -> new IllegalStateException("No session header for: " + sid));
            List<SessionEvent> existing = store.readAll(sid);
            SessionLog log = new SessionLog(sid, header);
            if (!existing.isEmpty()) {
                log.restore(existing);
            }
            return log;
        });
    }
}
