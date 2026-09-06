package cn.xiaofuge.deepseek.harness.domain.agent.model.valobj;

/**
 * Agent lifecycle state. Mirrors TS {@code AgentStatus}.
 * <ul>
 *   <li>{@code IDLE} — no driver is active</li>
 *   <li>{@code RUNNING} — waking input started and driver is draining turns</li>
 * </ul>
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public enum AgentStatus {
    IDLE,
    RUNNING
}
