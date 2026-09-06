package cn.xiaofuge.deepseek.harness.domain.agent.subagent;

import cn.xiaofuge.deepseek.harness.domain.agent.model.entity.AgentOptions;
import cn.xiaofuge.deepseek.harness.domain.agent.model.valobj.InboxTarget;
import cn.xiaofuge.deepseek.harness.domain.agent.service.AgentFactoryPort;
import cn.xiaofuge.deepseek.harness.domain.agent.service.ReactLoopAgent;
import cn.xiaofuge.deepseek.harness.domain.model.entity.Message;
import cn.xiaofuge.deepseek.harness.domain.model.entity.MessageSource;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.ContentBlock;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.TextBlock;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionEvent;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionLog;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.valobj.TurnEndReason;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * In-process subagent provider that forks the parent's completed-turn prefix
 * into the child session. Corresponds to TS {@code subagent-fork-in-process}.
 * <p>
 * The child inherits the parent's conversation context (up to the last
 * completed turn) instead of starting fresh. The current in-flight turn is
 * excluded because it's unbalanced and cannot be replayed.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class ForkInProcessProvider implements SubagentProvider {

    private final String providerName;
    private final SubagentRegistry registry;
    private final AgentFactoryPort agentFactory;
    private final AgentOptions defaultOptions;

    public ForkInProcessProvider(
            String providerName,
            SubagentRegistry registry,
            AgentFactoryPort agentFactory,
            AgentOptions defaultOptions
    ) {
        this.providerName = providerName;
        this.registry = registry;
        this.agentFactory = agentFactory;
        this.defaultOptions = defaultOptions;
    }

    @Override
    public String name() {
        return providerName;
    }

    @Override
    public SubagentCapabilities capabilities() {
        return new SubagentCapabilities(true, true, true);
    }

    @Override
    public CompletableFuture<SubagentResult> start(SubagentStartRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            String childSessionId = "child-" + UUID.randomUUID();
            AgentOptions childOptions = request.options() != null
                    ? request.options()
                    : defaultOptions;

            // Create child agent with fresh session
            ReactLoopAgent childAgent = agentFactory.create(
                    childSessionId,
                    childOptions,
                    request.parentCwd() != null ? request.parentCwd() : "."
            );

            // Deliver the prompt as a user message
            List<ContentBlock> promptBlocks = List.of(
                    new TextBlock(request.prompt() != null && !request.prompt().isEmpty()
                            ? request.prompt() : "(empty task)")
            );
            Message userMessage = Message.createUser(promptBlocks, new MessageSource.UserSource());
            childAgent.send(userMessage, InboxTarget.NEXT_TURN, true);

            // Wait for the turn to complete
            childAgent.whenIdle().join();

            // Collect the last assistant message
            List<ContentBlock> output = collectLastAssistantOutput(childAgent);

            // Determine stop reason
            String stopReason = determineStopReason(childAgent);

            // Dispose the child agent
            childAgent.dispose();

            return new SubagentResult(stopReason, output, registry.nextRunId());
        });
    }

    /**
     * Extract the completed-turn prefix from a parent session log.
     * Returns events up to and including the last {@code turn/end}.
     */
    public static List<SessionEvent> completedTurnPrefix(SessionLog parentSession) {
        List<SessionEvent> events = parentSession.events();
        int lastEndIdx = -1;
        for (int i = events.size() - 1; i >= 0; i--) {
            if (events.get(i) instanceof SessionEvent.TurnEnd) {
                lastEndIdx = i;
                break;
            }
        }
        if (lastEndIdx < 0) return List.of();
        return List.copyOf(events.subList(0, lastEndIdx + 1));
    }

    private List<ContentBlock> collectLastAssistantOutput(ReactLoopAgent agent) {
        var events = agent.session().events();
        TextBlock lastText = null;
        for (var event : events) {
            if (event instanceof SessionEvent.AssistantMessage am) {
                var msg = am.data().message();
                if (msg != null && msg.content() != null) {
                    for (var block : msg.content()) {
                        if (block instanceof TextBlock tb) {
                            lastText = tb;
                        }
                    }
                }
            }
        }
        return lastText != null ? List.of(lastText) : List.of();
    }

    private String determineStopReason(ReactLoopAgent agent) {
        var events = agent.session().events();
        TurnEndReason lastReason = null;
        for (var event : events) {
            if (event instanceof SessionEvent.TurnEnd te) {
                lastReason = te.reason();
            }
        }
        return SpawnInProcessProvider.mapStopReason(lastReason);
    }
}
