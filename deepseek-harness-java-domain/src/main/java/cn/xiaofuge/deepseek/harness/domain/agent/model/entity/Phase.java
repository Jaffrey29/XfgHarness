package cn.xiaofuge.deepseek.harness.domain.agent.model.entity;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Phase state machine for the agent driver. Mirrors TS {@code Phase}.
 * <p>
 * Three states:
 * <ul>
 *   <li>{@code Idle} — no active work; {@code lastTurn} tracks the last completed turn</li>
 *   <li>{@code Maintenance} — a non-turn maintenance task owns the agent</li>
 *   <li>{@code Running} — a driver is actively draining turns and steps</li>
 * </ul>
 *
 * <p>Every non-idle phase carries an {@link AtomicBoolean} abort flag and a
 * {@code wakeRequested} latch so a wake delivered during an abort or
 * maintenance is replayed when the phase converges back to idle.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public sealed interface Phase
        permits Phase.Idle, Phase.Maintenance, Phase.Running {

    /** No active work. */
    record Idle(long lastTurn) implements Phase {}

    /** Maintenance task owns the agent. */
    record Maintenance(
            AtomicBoolean abort,
            long lastTurn,
            AtomicBoolean wakeRequested
    ) implements Phase {
        /**
         * 处理Maintenance相关逻辑。
         */
        public Maintenance(long lastTurn) {
            this(new AtomicBoolean(false), lastTurn, new AtomicBoolean(false));
        }
    }

    /** Driver is actively running turns. */
    record Running(
            AtomicBoolean abort,
            long turn,
            long step,
            AtomicBoolean wakeRequested
    ) implements Phase {
        /**
         * 运行ning。
         */
        public Running(long lastTurn) {
            this(new AtomicBoolean(false), lastTurn, 0, new AtomicBoolean(false));
        }
    }
}
