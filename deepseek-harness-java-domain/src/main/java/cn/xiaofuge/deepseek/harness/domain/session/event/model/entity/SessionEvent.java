package cn.xiaofuge.deepseek.harness.domain.session.event.model.entity;

import cn.xiaofuge.deepseek.harness.domain.session.event.model.valobj.*;

import java.time.Instant;

/**
 * Discriminated union of all session events.
 * <p>
 * Each variant carries:
 * <ul>
 *   <li>{@code seq} — monotonically increasing sequence number (0-based, gapless)</li>
 *   <li>{@code time} — epoch milliseconds</li>
 *   <li>{@code type} — the {@link SessionEventType} discriminant</li>
 *   <li>type-specific {@code data} payload</li>
 *   <li>optional {@code surfaceIntent} — only for {@link SurfaceEventType} events</li>
 * </ul>
 * <p>
 * Non-surface events (boundaries, chunks, headers, etc.) never carry surface metadata.
 * Use {@link #isSurfaceEvent()} to check.
 * <p>
 * This is a sealed interface — all variants are listed here. The set may be extended
 * by adding new permitted records; readers MUST handle unknown required types by
 * rejecting reconstruction (unless the type is marked {@code ignorable}).
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public sealed interface SessionEvent permits
        SessionEvent.TurnStart,
        SessionEvent.TurnEnd,
        SessionEvent.StepStart,
        SessionEvent.StepEnd,
        SessionEvent.UserMessage,
        SessionEvent.AssistantChunk,
        SessionEvent.AssistantMessage,
        SessionEvent.ToolCall,
        SessionEvent.ToolResult,
        SessionEvent.TodoWrite,
        SessionEvent.RequestHeader,
        SessionEvent.RequestContext,
        SessionEvent.SessionEndSeed,
        SessionEvent.PlanModeChange,
        SessionEvent.AgentInboxSpliced,
        SessionEvent.Generic {

    long seq();
    Instant time();
    SessionEventType type();

    /** Whether this event is a surface event (carries surface metadata). */
    default boolean isSurfaceEvent() {
        return SurfaceEventType.isSurfaceEventType(type());
    }

    /** Surface intent (only present for surface events). */
    default SurfaceIntent surfaceIntent() {
        return null;
    }

    // ── Variants ────────────────────────────────────────────────

    /**
     * TurnStart 定义当前模块中的record契约与职责。
     */
    record TurnStart(long seq, Instant time) implements SessionEvent {
        @Override public SessionEventType type() { return SessionEventType.TURN_START; }
    }

    /**
     * TurnEnd 定义当前模块中的record契约与职责。
     */
    record TurnEnd(long seq, Instant time, TurnEndReason reason) implements SessionEvent {
        @Override public SessionEventType type() { return SessionEventType.TURN_END; }
    }

    /**
     * StepStart 定义当前模块中的record契约与职责。
     */
    record StepStart(long seq, Instant time, long turn, long step) implements SessionEvent {
        @Override public SessionEventType type() { return SessionEventType.STEP_START; }
    }

    /**
     * StepEnd 定义当前模块中的record契约与职责。
     */
    record StepEnd(long seq, Instant time, long turn, long step) implements SessionEvent {
        @Override public SessionEventType type() { return SessionEventType.STEP_END; }
    }

    /**
     * UserMessage 定义当前模块中的record契约与职责。
     */
    record UserMessage(
            long seq, Instant time,
            UserMessagePayload data,
            SurfaceIntent surfaceIntent
    ) implements SessionEvent {
        @Override public SessionEventType type() { return SessionEventType.USER_MESSAGE; }
    }

    /**
     * AssistantChunk 定义当前模块中的record契约与职责。
     */
    record AssistantChunk(
            long seq, Instant time,
            AssistantChunkPayload data
    ) implements SessionEvent {
        @Override public SessionEventType type() { return SessionEventType.ASSISTANT_CHUNK; }
    }

    /**
     * AssistantMessage 定义当前模块中的record契约与职责。
     */
    record AssistantMessage(
            long seq, Instant time,
            AssistantMessagePayload data,
            SurfaceIntent surfaceIntent
    ) implements SessionEvent {
        @Override public SessionEventType type() { return SessionEventType.ASSISTANT_MESSAGE; }
    }

    /**
     * ToolCall 定义当前模块中的record契约与职责。
     */
    record ToolCall(
            long seq, Instant time,
            ToolCallPayload data
    ) implements SessionEvent {
        @Override public SessionEventType type() { return SessionEventType.TOOL_CALL; }
    }

    /**
     * ToolResult 定义当前模块中的record契约与职责。
     */
    record ToolResult(
            long seq, Instant time,
            ToolResultPayload data,
            SurfaceIntent surfaceIntent
    ) implements SessionEvent {
        @Override public SessionEventType type() { return SessionEventType.TOOL_RESULT; }
    }

    /**
     * TodoWrite 定义当前模块中的record契约与职责。
     */
    record TodoWrite(long seq, Instant time, TodoWritePayload data) implements SessionEvent {
        @Override public SessionEventType type() { return SessionEventType.TODO_WRITE; }
    }

    /**
     * RequestHeader 定义当前模块中的record契约与职责。
     */
    record RequestHeader(long seq, Instant time, RequestHeaderPayload data) implements SessionEvent {
        @Override public SessionEventType type() { return SessionEventType.REQUEST_HEADER; }
    }

    /**
     * RequestContext 定义当前模块中的record契约与职责。
     */
    record RequestContext(long seq, Instant time, RequestContextPayload data) implements SessionEvent {
        @Override public SessionEventType type() { return SessionEventType.REQUEST_CONTEXT; }
    }

    /**
     * SessionEndSeed 定义当前模块中的record契约与职责。
     */
    record SessionEndSeed(long seq, Instant time) implements SessionEvent {
        @Override public SessionEventType type() { return SessionEventType.SESSION_END_SEED; }
    }

    /**
     * PlanModeChange 定义当前模块中的record契约与职责。
     */
    record PlanModeChange(long seq, Instant time, PlanModePayload data) implements SessionEvent {
        @Override public SessionEventType type() { return SessionEventType.PLAN_MODE_CHANGE; }
    }

    /**
     * AgentInboxSpliced 定义当前模块中的record契约与职责。
     */
    record AgentInboxSpliced(long seq, Instant time, InboxSplicedPayload data) implements SessionEvent {
        @Override public SessionEventType type() { return SessionEventType.AGENT_INBOX_SPLICED; }
    }

    /**
     * Generic fallback for ignorable/legacy events that do not need a typed payload.
     * Used for backward compatibility with old event types (task.submitted, etc.).
     */
    record Generic(
            long seq, Instant time,
            SessionEventType type,
            String payload
    ) implements SessionEvent {
        @Override
        public boolean isSurfaceEvent() {
            return false;
        }
    }
}
