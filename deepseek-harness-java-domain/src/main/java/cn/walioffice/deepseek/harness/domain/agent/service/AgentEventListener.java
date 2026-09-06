package cn.walioffice.deepseek.harness.domain.agent.service;

import cn.walioffice.deepseek.harness.domain.agent.model.valobj.AgentStatus;
import cn.walioffice.deepseek.harness.domain.llm.model.entity.Message;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Agent event dispatch interface. Mirrors TS {@code AgentEventDispatch}.
 * <p>
 * A simplified adaptation of the Cordis event system: listeners register
 * on the implementing class, and the agent loop calls emit/serial/waterfall
 * to dispatch events. The implementation is responsible for scope filtering
 * and error containment.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface AgentEventListener {

    /** Fire-and-forget notification (non-vetoing, errors contained). */
    void emit(String name, Object payload);

    /** Awaited in-order dispatch. */
    CompletableFuture<Void> serial(String name, Object payload);

    /** Around-middleware dispatch with a default action. */
    <T> CompletableFuture<T> waterfall(String name, Object payload, CompletableFuture<T> defaultAction);
}

/**
 * Convenience listener adapter — no-op defaults.
 */
class NoopAgentEventListener implements AgentEventListener {
    @Override public void emit(String name, Object payload) {}
    @Override public CompletableFuture<Void> serial(String name, Object payload) {
        return CompletableFuture.completedFuture(null);
    }
    @Override public <T> CompletableFuture<T> waterfall(String name, Object payload, CompletableFuture<T> defaultAction) {
        return defaultAction;
    }
}
