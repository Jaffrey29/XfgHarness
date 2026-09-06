package cn.walioffice.deepseek.harness.domain.session.event.service;

import cn.walioffice.deepseek.harness.domain.session.event.model.entity.SessionEvent;
import cn.walioffice.deepseek.harness.domain.session.event.model.entity.SessionHeader;
import java.util.List;

/**
 * Domain service for managing the session event log lifecycle.
 * <p>
 * This is the primary write-side interface for the session event system.
 * It coordinates the in-memory {@link SessionLog} with the durable
 * {@link ISessionEventStore}, ensuring events are persisted before they
 * are visible to read-side projections.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface ISessionEventLogService {

    /**
     * Create a new session log with the given header.
     * The header is persisted immediately.
     */
    SessionHeader createSession(SessionHeader header);

    /**
     * Append a single event to the session log and persist it.
     * The event's seq is assigned by the log (caller should set seq=0 or use factory).
     *
     * @return the appended event with the correct seq assigned
     */
    SessionEvent append(String sessionId, SessionEvent event);

    /**
     * Append multiple events atomically.
     */
    List<SessionEvent> appendBatch(String sessionId, List<SessionEvent> events);

    /**
     * Read all events for a session.
     */
    List<SessionEvent> readAll(String sessionId);

    /**
     * Read events after the given seq.
     */
    List<SessionEvent> readAfter(String sessionId, long afterSeq);

    /**
     * Get the session header.
     */
    SessionHeader getHeader(String sessionId);

    /**
     * Get the last seq for a session (-1 if empty).
     */
    long getLastSeq(String sessionId);

    /**
     * Restore a session log from persisted events (used by resume/replay).
     */
    void restore(String sessionId);
}
