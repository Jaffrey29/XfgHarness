package cn.walioffice.deepseek.harness.domain.agent.subagent;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class SubagentRegistryTest {

    @Test
    void registerAndResolve() {
        SubagentRegistry registry = new SubagentRegistry();
        SubagentProvider provider = new SubagentProvider() {
            @Override public String name() { return "test"; }
            @Override public java.util.concurrent.CompletableFuture<SubagentResult> start(SubagentStartRequest req) {
                return java.util.concurrent.CompletableFuture.completedFuture(
                        new SubagentResult("completed", List.of(), "run-1"));
            }
            @Override public SubagentCapabilities capabilities() {
                return new SubagentCapabilities(false, false, true);
            }
        };
        registry.register(provider);
        assertEquals("test", registry.resolve("test").name());
        assertTrue(registry.listProviderNames().contains("test"));
    }

    @Test
    void resolveUnknownThrows() {
        SubagentRegistry registry = new SubagentRegistry();
        assertThrows(IllegalArgumentException.class, () -> registry.resolve("nonexistent"));
    }

    @Test
    void unregister() {
        SubagentRegistry registry = new SubagentRegistry();
        SubagentProvider provider = new SubagentProvider() {
            @Override public String name() { return "test"; }
            @Override public java.util.concurrent.CompletableFuture<SubagentResult> start(SubagentStartRequest req) {
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            }
            @Override public SubagentCapabilities capabilities() {
                return new SubagentCapabilities(false, false, true);
            }
        };
        registry.register(provider);
        registry.unregister("test");
        assertTrue(registry.listProviderNames().isEmpty());
    }
}
