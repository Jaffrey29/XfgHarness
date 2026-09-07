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
import cn.xiaofuge.deepseek.harness.domain.session.event.model.valobj.TurnEndReason;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * In-process subagent provider that creates a fresh child agent with no
 * parent context inheritance. Corresponds to TS {@code subagent-spawn-in-process}.
 * <p>
 * The child gets its own session, system prompt, and tool registry. After the
 * child's turn completes, the result is collected and the child is disposed.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class SpawnInProcessProvider implements SubagentProvider {

    private final String providerName;
    private final SubagentRegistry registry;
    private final AgentFactoryPort agentFactory;
    private final AgentOptions defaultOptions;

    /**
     * 初始化进程内新会话子智能体提供者。
     * <p>流程：接收任务提示 → 创建全新 Agent 会话 → 执行并回传结果。</p>
     */
    public SpawnInProcessProvider(
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
    /**
     * 返回 name。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @return 方法执行结果。
     */


    @Override
    public String name() {
        return providerName;
    }
    /**
     * 返回 capabilities。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @return 方法执行结果。
     */


    @Override
    public SubagentCapabilities capabilities() {
        return new SubagentCapabilities(true, true, true);
    }
    /**
     * 启动目标对象。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param request 请求对象。
     * @return 方法执行结果。
     */


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

            // Collect the last assistant message from the session log
            List<ContentBlock> output = collectLastAssistantOutput(childAgent);

            // Determine stop reason from the last turn end event
            String stopReason = determineStopReason(childAgent);

            // Dispose the child agent
            childAgent.dispose();

            return new SubagentResult(stopReason, output, registry.nextRunId());
        });
    }

    /**
     * 处理collect Last Assistant Output。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param agent Agent参数。
     * @return 方法执行结果。
     */
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

    /**
     * 处理determine Stop Reason。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param agent Agent参数。
     * @return 方法执行结果。
     */
    private String determineStopReason(ReactLoopAgent agent) {
        var events = agent.session().events();
        TurnEndReason lastReason = null;
        for (var event : events) {
            if (event instanceof SessionEvent.TurnEnd te) {
                lastReason = te.reason();
            }
        }
        return mapStopReason(lastReason);
    }

    static String mapStopReason(TurnEndReason reason) {
        if (reason == null) return "error";
        if (reason instanceof TurnEndReason.Completed) return "completed";
        if (reason instanceof TurnEndReason.MaxTokens) return "max-tokens";
        if (reason instanceof TurnEndReason.Aborted) return "aborted";
        if (reason instanceof TurnEndReason.Blocked) return "refusal";
        return "error"; // error, interrupted
    }
}
