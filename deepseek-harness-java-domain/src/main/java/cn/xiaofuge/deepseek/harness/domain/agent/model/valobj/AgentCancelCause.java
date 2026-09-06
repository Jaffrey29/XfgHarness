package cn.xiaofuge.deepseek.harness.domain.agent.model.valobj;

/**
 * Why an agent activity was cancelled. Mirrors TS {@code AgentCancelCause}.
 * <p>
 * The first cause wins for any activity. {@code DISPOSED} means the agent
 * lifecycle is tearing down; other causes are caller-intent.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public sealed interface AgentCancelCause
        permits AgentCancelCause.UserCancel,
                AgentCancelCause.Disposed,
                AgentCancelCause.Maintenance {

    /** User explicitly cancelled the active turn or maintenance task. */
    record UserCancel(String reason) implements AgentCancelCause {}

    /** Agent lifecycle disposal triggered the cancellation. */
    record Disposed() implements AgentCancelCause {}

    /** Maintenance task was superseded by waking input. */
    record Maintenance() implements AgentCancelCause {}
}
