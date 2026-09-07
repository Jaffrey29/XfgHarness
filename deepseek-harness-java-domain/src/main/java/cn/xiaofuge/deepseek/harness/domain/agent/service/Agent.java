package cn.xiaofuge.deepseek.harness.domain.agent.service;

import cn.xiaofuge.deepseek.harness.domain.agent.model.entity.AgentOptions;
import cn.xiaofuge.deepseek.harness.domain.agent.model.valobj.AgentCancelCause;
import cn.xiaofuge.deepseek.harness.domain.agent.model.valobj.AgentStatus;
import cn.xiaofuge.deepseek.harness.domain.agent.model.valobj.InboxTarget;
import cn.xiaofuge.deepseek.harness.domain.model.entity.Message;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionLog;
import java.util.concurrent.CompletableFuture;

/**
 * Public live-agent handle. Mirrors TS {@code Agent} interface.
 *
 * <p>调用流程：模型输出 → 工具调用 → 结果回填 → 下一步模型调用；每一步写入会话事件。</p>
 * <p>示例：模型请求执行 fs_read 后，工具结果作为观察值进入下一轮模型输入。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface Agent {

    /** The single identity shared with the session. */
    String id();

    /** Provider route and model this agent's requests use. */
    AgentOptions options();

    /** The live session this agent drives. */
    SessionLog session();

    /** The agent-owned projection of durable pending work. */
    Inbox inbox();

    /** Current lifecycle state. */
    AgentStatus status();

    /**
     * Clear queued work (unless keepInbox) and abort the active turn.
     *
     * @param cause   the stable caller intent
     * @param keepInbox if true, preserve pending work
     */
    void cancel(AgentCancelCause cause, boolean keepInbox);

    /** Resolve after current activity reaches quiescence. */
    CompletableFuture<Void> whenIdle();

    /**
     * Run one non-turn maintenance task from idle phase.
     *
     * @param task operation whose fulfillment is preserved
     * @return the task result
     */
    <T> CompletableFuture<T> runMaintenance(java.util.function.Function<
            java.util.concurrent.atomic.AtomicBoolean, CompletableFuture<T>> task);

    /**
     * Route identified input to an inbox boundary and optionally wake the driver.
     *
     * @param message identified content
     * @param target  next-turn or next-step
     * @param wakeup  whether delivery may wake the driver
     */
    void send(Message message, InboxTarget target, boolean wakeup);

    /** Queue an ordinary follow-up turn and wake the driver. */
    default void followup(Message message) {
        send(message, InboxTarget.NEXT_TURN, true);
    }

    /** Submit steering for the nearest step. */
    default void steer(Message message) {
        send(message, InboxTarget.NEXT_STEP, true);
    }

    /** Queue model-facing context for the next pre-step without waking. */
    default void inject(Message message) {
        send(message, InboxTarget.NEXT_STEP, false);
    }
}
