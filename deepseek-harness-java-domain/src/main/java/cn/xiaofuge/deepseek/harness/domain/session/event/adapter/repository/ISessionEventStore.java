package cn.xiaofuge.deepseek.harness.domain.session.event.adapter.repository;

import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionEvent;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionHeader;
import java.util.List;
import java.util.Optional;

/**
 * Durable append-only store for session events and headers.
 * <p>
 * The store guarantees:
 * <ul>
 *   <li>Events are persisted in seq order (monotonic, gapless, 0-based per session).</li>
 *   <li>Once persisted, an event is never modified or deleted.</li>
 *   <li>Queries return events ordered by seq ascending.</li>
 * </ul>
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface ISessionEventStore {

    /**
     * Persist the session header. Called once at session creation.
     */
    void saveHeader(SessionHeader header);

    /**
     * Retrieve the session header.
     */
    Optional<SessionHeader> getHeader(String sessionId);

    /**
     * Append a single event. The event's seq must be the next sequential
     * value for this session.
     */
    void append(String sessionId, SessionEvent event);

    /**
     * Append multiple events atomically. Each event's seq must be sequential.
     */
    void appendBatch(String sessionId, List<SessionEvent> events);

    /**
     * Read all events for a session, ordered by seq ascending.
     */
    List<SessionEvent> readAll(String sessionId);

    /**
     * Read events after the given seq (exclusive), ordered by seq ascending.
     */
    List<SessionEvent> readAfter(String sessionId, long afterSeq);

    /**
     * Read events in the range [fromSeq, toSeq] (inclusive).
     */
    List<SessionEvent> readRange(String sessionId, long fromSeq, long toSeq);

    /**
     * Get the highest seq for a session, or -1 if no events exist.
     */
    long getLastSeq(String sessionId);

    /**
     * Count total events for a session.
     */
    long count(String sessionId);

    /**
     * List the most recent session headers, ordered by creation time descending.
     * Used by the console sidebar to surface history sessions.
     */
    List<SessionHeader> listHeaders(int limit);
}
