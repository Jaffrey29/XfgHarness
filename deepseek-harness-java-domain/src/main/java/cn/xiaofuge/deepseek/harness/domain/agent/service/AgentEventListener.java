package cn.xiaofuge.deepseek.harness.domain.agent.service;

import java.util.concurrent.CompletableFuture;

/**
 * AgentEventListener 表示智能体领域事件的监听契约。
 */
public interface AgentEventListener {

    /**
     * 处理emit。
     * <p>流程：校验入参 → 执行领域动作 → 返回处理结果。</p>
     * @param name 名称。
     * @return 领域处理结果。
     */
    void emit(String name, Object payload);

    /**
     * 处理serial。
     * <p>流程：校验入参 → 执行领域动作 → 返回处理结果。</p>
     * @param name 名称。
     * @return 领域处理结果。
     */
    CompletableFuture<Void> serial(String name, Object payload);

    /**
     * NoopAgentEventListener 监听NoopAgentEvent事件相关的领域事件。
     */
    <T> CompletableFuture<T> waterfall(String name, Object payload, CompletableFuture<T> defaultAction);
}

/**
 * NoopAgentEventListener 监听NoopAgentEvent事件相关的领域事件。
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
