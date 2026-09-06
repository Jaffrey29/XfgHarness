package cn.walioffice.deepseek.harness.domain.guard.timeout;

import cn.walioffice.deepseek.harness.domain.guard.model.valobj.ToolTimeoutResult;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Cooperative tool-call timeout enforcer.
 *
 * <p>A tool declares {@code timeoutMs} and promises to honor the signal reference;
 * this wrapper arms the deadline and maps expiry to a structured {@link ToolTimeoutResult}.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class ToolTimeoutPolicy {

    public static final String CODE = ToolTimeoutResult.CODE;

    /**
     * Arms a deadline for a single tool call. Blocks until either the tool completes
     * or the deadline fires (signaling through {@code signal}).
     *
     * @param signal        a reference the tool can check to cancel
     * @param timeoutMs     elapsed budget in milliseconds
     * @param toolRunnable  executable tool body
     * @return the tool result
     * @throws InterruptedException if the current thread is interrupted while waiting
     * @throws ToolTimeoutException when this policy's own timer wins
     */
    public static Object arm(java.util.concurrent.atomic.AtomicReference<Object> signal,
                             long timeoutMs,
                             Callable<Object> toolRunnable) throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);
        Object[] holder = new Object[2]; // [0]=result, [1]=error

        Thread worker = new Thread(() -> {
            try {
                holder[0] = toolRunnable.call();
            } catch (Throwable t) {
                holder[1] = t;
            } finally {
                done.countDown();
            }
        });
        worker.setDaemon(true);
        worker.start();

        boolean finished = done.await(timeoutMs, TimeUnit.MILLISECONDS);
        if (!finished) {
            signal.set(new TimeoutFired());
            done.await(500, TimeUnit.MILLISECONDS);
            throw new ToolTimeoutException(timeoutMs);
        }

        if (holder[1] != null) throw new RuntimeException((Throwable) holder[1]);
        return holder[0];
    }

    /** Holds the timeout-fired sentinel; tools check this to know their deadline won. */
    public static final class TimeoutFired {}

    /** Thrown when this policy's own timer wins. */
    public static class ToolTimeoutException extends RuntimeException {
        private final long timeoutMs;
        public ToolTimeoutException(long timeoutMs) {
            super("tool call timed out after " + timeoutMs + "ms");
            this.timeoutMs = timeoutMs;
        }
        public long timeoutMs() { return timeoutMs; }
    }
}
