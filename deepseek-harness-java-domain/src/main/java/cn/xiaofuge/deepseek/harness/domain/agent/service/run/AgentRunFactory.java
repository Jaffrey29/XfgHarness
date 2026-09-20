package cn.xiaofuge.deepseek.harness.domain.agent.service.run;

import cn.xiaofuge.deepseek.harness.domain.agent.contract.inbound.AgentRunLifecycle;
import cn.xiaofuge.deepseek.harness.domain.agent.contract.outbound.AgentEventListener;
import cn.xiaofuge.deepseek.harness.domain.agent.contract.outbound.RuntimeApprovalGateway;
import cn.xiaofuge.deepseek.harness.domain.agent.contract.outbound.UserQuestionGateway;
import cn.xiaofuge.deepseek.harness.domain.agent.model.entity.AgentOptions;
import cn.xiaofuge.deepseek.harness.domain.agent.model.valobj.ApprovalModeVO;
import cn.xiaofuge.deepseek.harness.domain.agent.service.Inbox;
import cn.xiaofuge.deepseek.harness.domain.agent.service.LoggingInboxNotifications;
import cn.xiaofuge.deepseek.harness.domain.agent.service.SystemPromptAssembler;
import cn.xiaofuge.deepseek.harness.domain.agent.service.run.tool.AgentToolCatalog;
import cn.xiaofuge.deepseek.harness.domain.agent.service.run.tool.AgentToolCatalog.AgentToolContext;
import cn.xiaofuge.deepseek.harness.domain.agent.service.compaction.CompactionEngine;
import cn.xiaofuge.deepseek.harness.domain.hooks.IHookService;
import cn.xiaofuge.deepseek.harness.domain.llm.adapter.port.ILlmRuntimePort;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionHeader;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionLog;
import cn.xiaofuge.deepseek.harness.domain.task.permission.adapter.port.IPermissionMatrixPort;
import cn.xiaofuge.deepseek.harness.domain.tool.adapter.ToolRegistry;
import cn.xiaofuge.deepseek.harness.domain.tool.service.MatrixRuntimeApprovalGate;
import cn.xiaofuge.deepseek.harness.domain.tool.service.ToolArgumentsParser;
import cn.xiaofuge.deepseek.harness.domain.tool.service.ToolCallExecutor;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

public class AgentRunFactory implements AgentRunLifecycle {

    private final ILlmRuntimePort llm;
    private final ToolRegistry sharedToolRegistry;
    private final SystemPromptAssembler systemPromptAssembler;
    private final UserQuestionGateway userQuestions;
    private final AgentToolCatalog toolCatalog;
    private final ToolArgumentsParser toolArgumentsParser;
    private final IHookService hookService;
    private final IPermissionMatrixPort permissionMatrixPort;
    private final RuntimeApprovalGateway approvalGateway;
    private final SessionLogFactory sessionLogFactory;
    private final AgentEventListenerFactory agentEventListenerFactory;
    private final CompactionEngine compactionEngine;
    private final ConcurrentMap<String, AgentRun> liveAgents = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, MatrixRuntimeApprovalGate> approvalGates = new ConcurrentHashMap<>();
    // 每个 Agent 的 cwd 引用：Agent 复用且请求换了工程目录时热更新，
    // 使 CwdResolvingFsPort 的相对路径解析跟随最新 cwd
    private final ConcurrentMap<String, java.util.concurrent.atomic.AtomicReference<String>> cwdRefs = new ConcurrentHashMap<>();

    public AgentRunFactory(
            ILlmRuntimePort llm,
            ToolRegistry sharedToolRegistry,
            SystemPromptAssembler systemPromptAssembler,
            UserQuestionGateway userQuestions,
            AgentToolCatalog toolCatalog,
            ToolArgumentsParser toolArgumentsParser,
            IHookService hookService,
            IPermissionMatrixPort permissionMatrixPort,
            RuntimeApprovalGateway approvalGateway,
            SessionLogFactory sessionLogFactory,
            AgentEventListenerFactory agentEventListenerFactory,
            CompactionEngine compactionEngine
    ) {
        this.llm = llm;
        this.sharedToolRegistry = sharedToolRegistry;
        this.systemPromptAssembler = systemPromptAssembler;
        this.userQuestions = userQuestions;
        this.toolCatalog = toolCatalog;
        this.toolArgumentsParser = toolArgumentsParser;
        this.hookService = hookService;
        this.permissionMatrixPort = permissionMatrixPort;
        this.approvalGateway = approvalGateway;
        this.sessionLogFactory = sessionLogFactory;
        this.agentEventListenerFactory = agentEventListenerFactory;
        this.compactionEngine = compactionEngine;
    }

    @Override
    public AgentRun create(String agentId, AgentOptions options, String cwd) {
        String sessionId = UUID.randomUUID().toString();
        SessionHeader header = SessionHeader.create(sessionId, Instant.now(), cwd, agentId);
        SessionLog session = sessionLogFactory.create(header);
        return wireAgent(agentId, options, cwd, session);
    }

    @Override
    public AgentRun resume(String agentId, AgentOptions options, String cwd, SessionLog session) {
        return wireAgent(agentId, options, cwd, session);
    }

    @Override
    public AgentRun get(String agentId) {
        return liveAgents.get(agentId);
    }

    @Override
    public void configureApprovalMode(String agentId, ApprovalModeVO mode) {
        MatrixRuntimeApprovalGate gate = approvalGates.get(agentId);
        if (gate != null) {
            gate.setApprovalMode(mode);
        }
    }

    @Override
    public void updateCwd(String agentId, String cwd) {
        if (cwd == null || cwd.isBlank()) {
            return;
        }
        java.util.concurrent.atomic.AtomicReference<String> reference = cwdRefs.get(agentId);
        if (reference != null) {
            reference.set(cwd);
        }
    }

    public java.util.Collection<AgentRun> all() {
        return liveAgents.values();
    }

    @Override
    public CompletableFuture<Void> disposeAll() {
        return CompletableFuture.allOf(
                liveAgents.values().stream()
                        .map(AgentRun::dispose)
                        .toArray(CompletableFuture[]::new)
        );
    }

    private AgentRun wireAgent(String agentId, AgentOptions options, String cwd, SessionLog session) {
        Inbox inbox = new Inbox(session, LoggingInboxNotifications.INSTANCE);
        String sessionId = session.sessionId();
        AtomicReference<AgentOptions> optionsReference = new AtomicReference<>(options);
        java.util.concurrent.atomic.AtomicReference<String> cwdReference =
                new java.util.concurrent.atomic.AtomicReference<>(cwd);
        cwdRefs.put(agentId, cwdReference);
        ToolRegistry toolRegistry = toolCatalog.createRegistry(
                new AgentToolContext(agentId, sessionId, cwd, session, userQuestions, optionsReference::get, cwdReference),
                sharedToolRegistry
        );

        ToolCallExecutor executor = new ToolCallExecutor(
                toolRegistry,
                session,
                10,
                toolArgumentsParser
        );
        if (hookService != null) {
            executor.setHookService(hookService);
        }
        if (permissionMatrixPort != null && approvalGateway != null) {
            MatrixRuntimeApprovalGate gate = new MatrixRuntimeApprovalGate(
                    permissionMatrixPort,
                    agentId,
                    (request, ignored) -> approvalGateway.requestApproval(request)
            );
            gate.setApprovalMode(options.approvalMode());
            executor.setApprovalGate(gate);
            approvalGates.put(agentId, gate);
        }

        AgentEventListener agentEventListener = agentEventListenerFactory == null
                ? null
                : agentEventListenerFactory.create(session.sessionId());

        ReactLoopAgent agent = new ReactLoopAgent(
                agentId,
                options,
                cwd,
                session,
                inbox,
                llm,
                toolRegistry,
                executor,
                systemPromptAssembler,
                agentEventListener,
                compactionEngine,
                optionsReference::set
        );
        liveAgents.put(agentId, agent);
        return agent;
    }

    @FunctionalInterface
    public interface SessionLogFactory {
        SessionLog create(SessionHeader header);
    }

    @FunctionalInterface
    public interface AgentEventListenerFactory {
        AgentEventListener create(String sessionId);
    }
}
