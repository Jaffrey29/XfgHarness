package cn.walioffice.deepseek.harness.domain.agent.service;

import cn.walioffice.deepseek.harness.domain.agent.model.entity.AgentOptions;
import cn.walioffice.deepseek.harness.domain.agent.model.valobj.InboxTarget;
import cn.walioffice.deepseek.harness.domain.llm.adapter.port.ILlmRuntimePort;
import cn.walioffice.deepseek.harness.domain.llm.adapter.LlmAdapter;
import cn.walioffice.deepseek.harness.domain.llm.model.entity.Message;
import cn.walioffice.deepseek.harness.domain.llm.model.entity.MessageSource;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.ContentBlock;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.FinishReason;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.GenerateOptions;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.StreamChunk;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.TextBlock;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.ToolSchema;
import cn.walioffice.deepseek.harness.domain.session.event.model.entity.SessionHeader;
import cn.walioffice.deepseek.harness.domain.session.event.model.entity.SessionEvent;
import cn.walioffice.deepseek.harness.domain.session.event.model.entity.SessionLog;
import cn.walioffice.deepseek.harness.domain.session.event.model.valobj.TurnEndReason;
import cn.walioffice.deepseek.harness.domain.tool.adapter.ToolRegistry;
import cn.walioffice.deepseek.harness.domain.tool.model.entity.ToolDefinition;
import cn.walioffice.deepseek.harness.domain.tool.model.entity.ToolExecutionInput;
import cn.walioffice.deepseek.harness.domain.tool.model.valobj.ToolExecutionMode;
import cn.walioffice.deepseek.harness.domain.tool.service.ToolCallExecutor;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReactLoopAgentTest {

    @Test
    void continuesTruncatedAssistantOutputInSameTurn() {
        SessionLog session = new SessionLog(
                "session-1",
                SessionHeader.create("session-1", Instant.now(), ".", "default")
        );
        Inbox inbox = new Inbox(session, new NoopInboxNotifications());
        CapturingLlm llm = new CapturingLlm();

        ReactLoopAgent agent = new ReactLoopAgent(
                "agent-1",
                new AgentOptions("deepseek", "deepseek-chat", 8, null, null),
                ".",
                session,
                inbox,
                llm,
                new EmptyToolRegistry(),
                new ToolCallExecutor(new EmptyToolRegistry(), session, 1),
                new SystemPromptAssembler() {
                    @Override
                    public CompletableFuture<PromptAssembly> assemble(
                            String agentId,
                            AgentOptions options,
                            String cwd,
                            List<ToolSchema> tools
                    ) {
                        return CompletableFuture.completedFuture(
                                new PromptAssembly("system", List.of())
                        );
                    }
                },
                new NoopAgentEventListener()
        );

        agent.send(Message.createUser(
                        List.of(new TextBlock("write a long answer")),
                        new MessageSource.UserSource()
                ),
                InboxTarget.NEXT_TURN,
                true
        );
        agent.whenIdle().join();

        assertEquals(2, llm.requests.size());
        assertTrue(llm.requests.get(1).messages().stream()
                .anyMatch(message -> message.content().stream()
                        .anyMatch(block -> block instanceof TextBlock text
                                && text.text().equals(ReactLoopAgent.CONTINUATION_PROMPT))
                        && message.source() instanceof MessageSource.PluginSource plugin
                        && "react-loop".equals(plugin.plugin())));
        assertEquals(2, session.deriveMessages().stream()
                .filter(message -> "assistant".equals(message.role()))
                .count());

        TurnEndReason end = session.events().stream()
                .filter(event -> event instanceof SessionEvent.TurnEnd)
                .map(event -> ((SessionEvent.TurnEnd) event).reason())
                .reduce((first, second) -> second)
                .orElseThrow();
        assertTrue(end instanceof TurnEndReason.Completed);
    }

    private static final class CapturingLlm implements ILlmRuntimePort {
        private final List<GenerateOptions> requests = new CopyOnWriteArrayList<>();

        @Override
        public void registerAdapter(String provider, LlmAdapter adapter) { }

        @Override
        public void unregisterAdapter(String provider) { }

        @Override
        public List<LlmAdapter.LlmProviderInfo> listProviders() {
            return List.of();
        }

        @Override
        public List<LlmAdapter.LlmModelInfo> listModels(String provider) {
            return List.of();
        }

        @Override
        public LlmAdapter.LlmResolvedModelInfo resolveModel(String provider, String model) {
            return new LlmAdapter.LlmResolvedModelInfo(
                    provider,
                    model,
                    model,
                    null,
                    List.of("text"),
                    new LlmAdapter.LlmModelContext(128_000),
                    8,
                    null
            );
        }

        @Override
        public Flow.Publisher<StreamChunk> stream(GenerateOptions options) {
            requests.add(options);
            int index = requests.size() - 1;
            List<StreamChunk> chunks = List.of(
                    new StreamChunk.BlockStart(0, "text"),
                    new StreamChunk.TextDelta(0, index == 0 ? "first " : "second"),
                    new StreamChunk.BlockEnd(0, new TextBlock(index == 0 ? "first " : "second")),
                    new StreamChunk.Finish(index == 0
                    ? new FinishReason.MaxTokens()
                    : new FinishReason.Stop())
            );
            return subscriber -> {
                subscriber.onSubscribe(new Flow.Subscription() {
                    @Override public void request(long n) { }
                    @Override public void cancel() { }
                });
                chunks.forEach(subscriber::onNext);
                subscriber.onComplete();
            };
        }
    }

    private static final class EmptyToolRegistry implements ToolRegistry {
        @Override
        public Runnable register(ToolDefinition definition) {
            return () -> { };
        }

        @Override
        public Optional<ToolDefinition> lookup(String name) {
            return Optional.empty();
        }

        @Override
        public Collection<ToolDefinition> all() {
            return List.of();
        }

        @Override
        public ToolExecutionMode executionMode(ToolExecutionInput exec) {
            return ToolExecutionMode.EXCLUSIVE;
        }

        @Override
        public List<ToolSchema> schemas() {
            return List.of();
        }
    }

    private static final class NoopInboxNotifications implements InboxNotifications {
        @Override
        public void inserted(Message message) { }

        @Override
        public void discarded(Message message) { }

        @Override
        public void claimed(Message message, int turn) { }
    }
}
