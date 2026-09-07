package cn.xiaofuge.deepseek.harness.domain.session.event.service;

import cn.xiaofuge.deepseek.harness.domain.model.entity.Message;
import cn.xiaofuge.deepseek.harness.domain.llm.model.valobj.TokenUsage;
import cn.xiaofuge.deepseek.harness.domain.model.entity.MessageSource;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.TextBlock;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionEvent;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionLog;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.valobj.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Factory for creating session events with correct defaults.
 * <p>
 * All factory methods produce events with seq=0 (the {@link SessionLog}
 * will reassign the correct seq on append). Callers only need to provide
 * the event-specific data and a timestamp.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public final class SessionEventFactory {

    /**
     * 处理Session Event Factory相关逻辑。
     */
    private SessionEventFactory() {}

    /**
     * 处理now相关逻辑。
     */
    private static Instant now() {
        return Instant.now();
    }

    // ── Turn / Step boundaries ───────────────────────────────────

    /**
     * 处理turn Start相关逻辑。
     */
    public static SessionEvent.TurnStart turnStart() {
        return new SessionEvent.TurnStart(0, now());
    }

    /**
     * 处理turn Start相关逻辑。
     */
    public static SessionEvent.TurnStart turnStart(Instant time) {
        return new SessionEvent.TurnStart(0, time);
    }

    /**
     * 处理turn End相关逻辑。
     */
    public static SessionEvent.TurnEnd turnEnd(TurnEndReason reason) {
        return new SessionEvent.TurnEnd(0, now(), reason);
    }

    /**
     * 处理turn End相关逻辑。
     */
    public static SessionEvent.TurnEnd turnEnd(Instant time, TurnEndReason reason) {
        return new SessionEvent.TurnEnd(0, time, reason);
    }

    /**
     * 处理step Start相关逻辑。
     */
    public static SessionEvent.StepStart stepStart(long turn, long step) {
        return new SessionEvent.StepStart(0, now(), turn, step);
    }

    /**
     * 处理step Start相关逻辑。
     */
    public static SessionEvent.StepStart stepStart(Instant time, long turn, long step) {
        return new SessionEvent.StepStart(0, time, turn, step);
    }

    /**
     * 处理step End相关逻辑。
     */
    public static SessionEvent.StepEnd stepEnd(long turn, long step) {
        return new SessionEvent.StepEnd(0, now(), turn, step);
    }

    /**
     * 处理step End相关逻辑。
     */
    public static SessionEvent.StepEnd stepEnd(Instant time, long turn, long step) {
        return new SessionEvent.StepEnd(0, time, turn, step);
    }

    // ── Messages ─────────────────────────────────────────────────

    /**
     * Create an assistant chunk event (ignorable, for replay fidelity).
     */
    public static SessionEvent.AssistantChunk assistantChunk(long turn, long step, String chunk, String kind) {
        return new SessionEvent.AssistantChunk(0, now(),
                new AssistantChunkPayload(turn, step, chunk, kind));
    }

    /**
     * Create a user message event from a {@link Message} (role=user).
     */
    public static SessionEvent.UserMessage userMessage(long turn, long step, Message message) {
        return new SessionEvent.UserMessage(0, now(),
                new UserMessagePayload(turn, step, message),
                new SurfaceIntent(SurfaceOp.append(), null));
    }

    /**
     * Create a user message event with a custom surface intent (e.g. Replace for compaction).
     */
    public static SessionEvent.UserMessage userMessage(long turn, long step, Message message, SurfaceIntent intent) {
        return new SessionEvent.UserMessage(0, now(),
                new UserMessagePayload(turn, step, message),
                intent);
    }

    /**
     * Create a user message event from a plain string (convenience for legacy callers).
     */
    public static SessionEvent.UserMessage userMessage(String content) {
        Message msg = Message.createUser(
                List.of(new TextBlock(content)),
                new MessageSource.UserSource()
        );
        return userMessage(0, 0, msg);
    }

    /**
     * Create an injected user message (non-waking context).
     */
    public static SessionEvent.UserMessage injectedUserMessage(long turn, long step, Message message) {
        return new SessionEvent.UserMessage(0, now(),
                new UserMessagePayload(turn, step, message),
                new SurfaceIntent(SurfaceOp.append(), null));
    }

    /**
     * Create an assistant message event from a {@link Message} (role=assistant).
     */
    public static SessionEvent.AssistantMessage assistantMessage(
            long turn, long step, Message message, TokenUsage usage
    ) {
        return new SessionEvent.AssistantMessage(0, now(),
                new AssistantMessagePayload(turn, step, message, usage),
                new SurfaceIntent(SurfaceOp.append(), null));
    }

    // ── Tool calls ───────────────────────────────────────────────

    /**
     * 处理tool Call相关逻辑。
     */
    public static SessionEvent.ToolCall toolCall(long turn, long step, String callId, String name, String arguments) {
        return new SessionEvent.ToolCall(0, now(),
                new ToolCallPayload(turn, step, callId, name, arguments));
    }

    /**
     * 处理tool Result相关逻辑。
     */
    public static SessionEvent.ToolResult toolResult(
            long turn, long step, Message message,
            Map<String, Object> error, Map<String, Object> meta
    ) {
        return new SessionEvent.ToolResult(0, now(),
                new ToolResultPayload(turn, step, message, error, meta),
                new SurfaceIntent(SurfaceOp.append(), null));
    }

    // ── Todo ─────────────────────────────────────────────────────

    /**
     * 处理todo Write相关逻辑。
     */
    public static SessionEvent.TodoWrite todoWrite(List<TodoItemPayload> todos) {
        return new SessionEvent.TodoWrite(0, now(), new TodoWritePayload(todos));
    }

    // ── Request metadata ─────────────────────────────────────────

    /**
     * 处理request Header相关逻辑。
     */
    public static SessionEvent.RequestHeader requestHeader(
            Map<String, Object> config, Map<String, Object> adapterDefaults,
            String system, List<Map<String, Object>> tools
    ) {
        return new SessionEvent.RequestHeader(0, now(),
                new RequestHeaderPayload(config, adapterDefaults, system, tools));
    }

    /**
     * 处理request Context相关逻辑。
     */
    public static SessionEvent.RequestContext requestContext(String provider, String model, Long contextWindow) {
        return new SessionEvent.RequestContext(0, now(),
                new RequestContextPayload(provider, model, contextWindow));
    }

    // ── Session lifecycle ────────────────────────────────────────

    /**
     * 处理session End Seed相关逻辑。
     */
    public static SessionEvent.SessionEndSeed sessionEndSeed() {
        return new SessionEvent.SessionEndSeed(0, now());
    }

    // ── Agent inbox ──────────────────────────────────────────────

    /**
     * 处理inbox Spliced相关逻辑。
     */
    public static SessionEvent.AgentInboxSpliced inboxSpliced(
            String target, int start, int removedCount,
            List<Message> inserted, String outcome
    ) {
        return new SessionEvent.AgentInboxSpliced(0, now(),
                new InboxSplicedPayload(target, start, removedCount, inserted, outcome));
    }

    // ── Plan mode ────────────────────────────────────────────────

    /**
     * 处理plan Mode Enter相关逻辑。
     */
    public static SessionEvent.PlanModeChange planModeEnter() {
        return new SessionEvent.PlanModeChange(0, now(), new PlanModePayload("enter", null));
    }

    /**
     * 处理plan Mode Exit相关逻辑。
     */
    public static SessionEvent.PlanModeChange planModeExit(String plan) {
        return new SessionEvent.PlanModeChange(0, now(), new PlanModePayload("exit", plan));
    }

    // ── Generic ──────────────────────────────────────────────────

    /**
     * 处理generic相关逻辑。
     */
    public static SessionEvent.Generic generic(SessionEventType type, String payload) {
        return new SessionEvent.Generic(0, now(), type, payload);
    }

    // ── Turn end reason helpers ──────────────────────────────────

    /**
     * 处理completed相关逻辑。
     */
    public static TurnEndReason completed() { return new TurnEndReason.Completed(); }
    /**
     * 处理max Tokens相关逻辑。
     */
    public static TurnEndReason maxTokens() { return new TurnEndReason.MaxTokens(); }
    /**
     * 处理blocked相关逻辑。
     */
    public static TurnEndReason blocked() { return new TurnEndReason.Blocked(); }
    /**
     * 处理aborted相关逻辑。
     */
    public static TurnEndReason aborted(String reason) { return new TurnEndReason.Aborted(reason); }
    /**
     * 处理error相关逻辑。
     */
    public static TurnEndReason error(String message) { return new TurnEndReason.Error(message, "UNKNOWN"); }
    /**
     * 处理error相关逻辑。
     */
    public static TurnEndReason error(String message, String code) { return new TurnEndReason.Error(message, code); }
    /**
     * 处理interrupted相关逻辑。
     */
    public static TurnEndReason interrupted() { return new TurnEndReason.Interrupted(); }
}
