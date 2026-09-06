package cn.walioffice.deepseek.harness.domain.hooks.service;

import cn.walioffice.deepseek.harness.domain.hooks.adapter.port.IHookRunnerPort;
import cn.walioffice.deepseek.harness.domain.hooks.model.valobj.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class HookServiceTest {

    private static HookOutput blocked() {
        return new HookOutput(2, "", "{}",
                Optional.empty(), Optional.empty(),
                Optional.of(HookOutput.HookDecision.BLOCK), Optional.of("blocked by test"),
                Optional.empty(), Optional.empty());
    }

    private static HookOutput allow() {
        return new HookOutput(0, "", "{}",
                Optional.of(true), Optional.empty(),
                Optional.of(HookOutput.HookDecision.ALLOW), Optional.empty(),
                Optional.empty(), Optional.empty());
    }

    @Test
    void noHooksReturnsEmpty() {
        IHookRunnerPort noopRunner = (hook, payload, env) -> allow();
        HookService service = new HookService(noopRunner);
        var outcome = service.runHooks(HookDialect.CLAUDE_CODE, HookPoint.PRE_TOOL_USE, "echo", "{}", Map.of());
        assertTrue(outcome.decision().isEmpty());
        assertFalse(outcome.stop());
    }

    @Test
    void blockDecisionMerged() {
        IHookRunnerPort blockRunner = (hook, payload, env) -> blocked();
        HookService service = new HookService(blockRunner);
        service.registerMatchers(HookDialect.CLAUDE_CODE, HookPoint.PRE_TOOL_USE,
                List.of(new MatcherGroup("*", List.of(new CommandHook("exit 2", null)))));
        var outcome = service.runHooks(HookDialect.CLAUDE_CODE, HookPoint.PRE_TOOL_USE, "echo", "{}", Map.of());
        assertTrue(outcome.decision().isPresent());
        assertEquals(HookOutput.HookDecision.BLOCK, outcome.decision().get());
    }

    @Test
    void matcherFilters() {
        IHookRunnerPort blockRunner = (hook, payload, env) -> blocked();
        HookService service = new HookService(blockRunner);
        service.registerMatchers(HookDialect.CLAUDE_CODE, HookPoint.PRE_TOOL_USE,
                List.of(new MatcherGroup("shell_execute", List.of(new CommandHook("exit 2", null)))));
        var blocked = service.runHooks(HookDialect.CLAUDE_CODE, HookPoint.PRE_TOOL_USE, "shell_execute", "{}", Map.of());
        assertTrue(blocked.decision().isPresent());
        var notBlocked = service.runHooks(HookDialect.CLAUDE_CODE, HookPoint.PRE_TOOL_USE, "fs_read", "{}", Map.of());
        assertTrue(notBlocked.decision().isEmpty());
    }
}
