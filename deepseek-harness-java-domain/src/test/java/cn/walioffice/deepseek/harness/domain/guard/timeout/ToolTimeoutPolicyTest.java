package cn.walioffice.deepseek.harness.domain.guard.timeout;

import static org.junit.jupiter.api.Assertions.*;

import cn.walioffice.deepseek.harness.domain.guard.timeout.ToolTimeoutPolicy.ToolTimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ToolTimeoutPolicyTest {

    @Test
    void returnsResultWhenToolCompletesInTime() throws Exception {
        AtomicReference<Object> signal = new AtomicReference<>(null);
        Object result = ToolTimeoutPolicy.arm(signal, 2000, () -> "done");
        assertEquals("done", result);
        assertNull(signal.get());
    }

    @Test
    void throwsTimeoutWhenToolExceedsDeadline() throws Exception {
        AtomicReference<Object> signal = new AtomicReference<>(null);
        CountDownLatch entered = new CountDownLatch(1);
        ToolTimeoutException ex = assertThrows(ToolTimeoutException.class, () ->
                ToolTimeoutPolicy.arm(signal, 100, () -> {
                    entered.countDown();
                    Thread.sleep(3000);
                    return "never";
                })
        );
        assertEquals(100, ex.timeoutMs());
        // signal should have been set to TimeoutFired
        assertInstanceOf(ToolTimeoutPolicy.TimeoutFired.class, signal.get());
    }

    @Test
    void wrapsToolExceptionInRuntimeException() {
        AtomicReference<Object> signal = new AtomicReference<>(null);
        assertThrows(RuntimeException.class, () ->
                ToolTimeoutPolicy.arm(signal, 2000, () -> {
                    throw new IllegalStateException("boom");
                })
        );
    }
}
