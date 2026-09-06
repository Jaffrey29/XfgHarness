package cn.walioffice.deepseek.harness.domain.agent.service;

import cn.walioffice.deepseek.harness.domain.agent.model.entity.AgentOptions;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScopedSystemPromptAssemblerTest {

    @Test
    void includesRuntimeContextInRenderedPrompt() throws Exception {
        var assembler = new ScopedSystemPromptAssembler("identity", "persona", List.of());
        assembler.registerContext(new ScopedSystemPromptAssembler.PromptContext(
                "test:runtime",
                10,
                (java.util.function.Function<ScopedSystemPromptAssembler.AssembleContext, String>)
                        context -> "cwd=" + context.cwd()
        ));

        var prompt = assembler.assemble(
                "agent", new AgentOptions("provider", "model", 100, null, null), "/workspace", List.of()
        ).get(1, TimeUnit.SECONDS);

        assertTrue(prompt.system().contains("Current runtime context."));
        assertTrue(prompt.system().contains("cwd=/workspace"));
        assertTrue(prompt.system().contains("identity"));
        assertEquals(0, prompt.tools().size());
    }
}
