package cn.walioffice.deepseek.harness.domain.tool.service;

import cn.walioffice.deepseek.harness.domain.llm.model.valobj.TextBlock;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.ToolCallBlock;
import cn.walioffice.deepseek.harness.domain.session.event.model.entity.SessionHeader;
import cn.walioffice.deepseek.harness.domain.session.event.model.entity.SessionLog;
import cn.walioffice.deepseek.harness.domain.tool.adapter.ToolRegistry;
import cn.walioffice.deepseek.harness.domain.tool.model.entity.ToolDefinition;
import cn.walioffice.deepseek.harness.domain.tool.model.entity.ToolExecutionInput;
import cn.walioffice.deepseek.harness.domain.tool.model.entity.ToolExecutionResult;
import cn.walioffice.deepseek.harness.domain.tool.model.entity.ToolRunContext;
import cn.walioffice.deepseek.harness.domain.tool.model.valobj.ToolExecutionMode;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolCallExecutorTest {

    @Test
    void removesCompletedParallelCallsFromInFlightTracking() throws Exception {
        SessionLog session = new SessionLog(
                "session-parallel-tools",
                SessionHeader.create("session-parallel-tools", Instant.now(), ".", "agent-parallel-tools")
        );
        ToolCallExecutor executor = new ToolCallExecutor(new InstantToolRegistry(), session, 2);
        List<ToolCallBlock> calls = List.of(
                new ToolCallBlock("call-1", "instant-tool", "{}"),
                new ToolCallBlock("call-2", "instant-tool", "{}")
        );

        Boolean concluded = CompletableFuture.supplyAsync(
                        () -> executor.execute(1, 1, calls, new AtomicBoolean(false), ignored -> { }))
                .get(1, TimeUnit.SECONDS);

        assertEquals(false, concluded);
        assertEquals(4, session.events().size());
    }

    @Test
    void parsesJsonArgumentsBeforeToolExecution() throws Exception {
        SessionLog session = new SessionLog(
                "session-arguments",
                SessionHeader.create("session-arguments", Instant.now(), ".", "agent-arguments")
        );
        java.util.concurrent.atomic.AtomicReference<Object> received = new java.util.concurrent.atomic.AtomicReference<>();
        ToolDefinition tool = new ToolDefinition() {
            @Override public String name() { return "argument-tool"; }
            @Override public String description() { return "Records arguments"; }
            @Override public java.util.Map<String, Object> parameters() {
                return java.util.Map.of("type", "object");
            }
            @Override public java.util.Map<String, Object> outputSchema() {
                return java.util.Map.of("type", "object");
            }
            @Override public CompletableFuture<ToolExecutionResult> execute(Object args, ToolRunContext ctx) {
                received.set(args);
                return CompletableFuture.completedFuture(
                        ToolExecutionResult.ok(List.of(new TextBlock("done"))));
            }
        };
        ToolRegistry registry = new ToolRegistry() {
            @Override public Runnable register(ToolDefinition definition) { return () -> { }; }
            @Override public Optional<ToolDefinition> lookup(String name) {
                return Optional.of(tool);
            }
            @Override public java.util.Collection<ToolDefinition> all() { return List.of(tool); }
            @Override public ToolExecutionMode executionMode(ToolExecutionInput exec) {
                return ToolExecutionMode.EXCLUSIVE;
            }
        };
        ToolCallExecutor executor = new ToolCallExecutor(
                registry,
                session,
                1,
                raw -> java.util.Map.of("path", "/tmp/example")
        );

        executor.execute(1, 1, List.of(new ToolCallBlock("call-1", "argument-tool", "{\"path\":\"/tmp/example\"}")),
                new AtomicBoolean(false), ignored -> { });

        assertTrue(received.get() instanceof java.util.Map<?, ?>);
        assertEquals("/tmp/example", ((java.util.Map<?, ?>) received.get()).get("path"));
    }

    private static final class InstantToolRegistry implements ToolRegistry {
        private final ToolDefinition tool = new ToolDefinition() {
            @Override
            public String name() {
                return "instant-tool";
            }

            @Override
            public String description() {
                return "Returns an already completed result";
            }

            @Override
            public java.util.Map<String, Object> parameters() {
                return java.util.Map.of("type", "object");
            }

            @Override
            public java.util.Map<String, Object> outputSchema() {
                return java.util.Map.of("type", "string");
            }

            @Override
            public CompletableFuture<ToolExecutionResult> execute(Object args, ToolRunContext exec) {
                return CompletableFuture.completedFuture(
                        ToolExecutionResult.ok(List.of(new TextBlock("done"))));
            }
        };

        @Override
        public Runnable register(ToolDefinition definition) {
            return () -> { };
        }

        @Override
        public Optional<ToolDefinition> lookup(String name) {
            return "instant-tool".equals(name) ? Optional.of(tool) : Optional.empty();
        }

        @Override
        public Collection<ToolDefinition> all() {
            return List.of(tool);
        }

        @Override
        public ToolExecutionMode executionMode(ToolExecutionInput exec) {
            return ToolExecutionMode.PARALLEL;
        }
    }
}
