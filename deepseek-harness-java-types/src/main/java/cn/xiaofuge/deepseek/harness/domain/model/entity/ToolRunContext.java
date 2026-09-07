package cn.xiaofuge.deepseek.harness.domain.model.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 注册表接受 {@link ToolExecution} 后，传递给工具实现的运行时上下文。
 * 对应 TS 侧 {@code ToolRunContext}。
 *
 * <p>Provides:
 * <ul>
 *   <li>{@link #deferContext(Message)} — 延迟一条上下文消息，直到工具最终结果
 *       回到 Agent 循环后再输出</li>
 *   <li>{@link #concludeTurn()} — 将成功结果标记为当前回合的终止结果</li>
 * </ul>
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public final class ToolRunContext {

    private final ToolExecution execution;
    private final List<Message> deferredContexts = new ArrayList<>();
    private final AtomicBoolean turnConcluded = new AtomicBoolean(false);

    /**
     * 处理Tool Run Context相关逻辑。
     */
    public ToolRunContext(ToolExecution execution) {
        this.execution = execution;
    }

/** 本次执行的身份标识。 */
    public ToolExecution execution() {
        return execution;
    }

/** 延迟一条上下文消息，供 Agent 循环在工具结果后输出。 */
    public void deferContext(Message context) {
        if (context != null) {
            deferredContexts.add(context);
        }
    }

/** 将当前工具结果标记为本回合的终止结果。 */
    public void concludeTurn() {
        turnConcluded.set(true);
    }

/** 是否已经调用过 concludeTurn()。 */
    public boolean isTurnConcluded() {
        return turnConcluded.get();
    }

/** 清空延迟上下文消息，由调度器在处理结果后调用。 */
    public List<Message> drainDeferredContexts() {
        synchronized (deferredContexts) {
            return List.copyOf(deferredContexts);
        }
    }
}
