package cn.walioffice.deepseek.harness.infrastructure.adapter.agent;

import cn.walioffice.deepseek.harness.domain.agent.ask.AskUserTool;
import cn.walioffice.deepseek.harness.domain.agent.ask.UserQuestionsPort;
import cn.walioffice.deepseek.harness.domain.agent.model.entity.AgentOptions;
import cn.walioffice.deepseek.harness.domain.plan.ExitPlanModeTool;
import cn.walioffice.deepseek.harness.domain.plan.model.valobj.PlanModeState;
import cn.walioffice.deepseek.harness.domain.plan.service.PlanModeService;
import cn.walioffice.deepseek.harness.domain.agent.service.Agent;
import cn.walioffice.deepseek.harness.domain.agent.service.AgentEventListener;
import cn.walioffice.deepseek.harness.domain.agent.service.Inbox;
import cn.walioffice.deepseek.harness.domain.agent.service.LoggingInboxNotifications;
import cn.walioffice.deepseek.harness.domain.agent.service.ReactLoopAgent;
import cn.walioffice.deepseek.harness.domain.agent.service.SystemPromptAssembler;
import cn.walioffice.deepseek.harness.domain.agent.subagent.SubagentRegistry;
import cn.walioffice.deepseek.harness.domain.agent.subagent.SubagentTool;
import cn.walioffice.deepseek.harness.domain.hooks.IHookService;
import cn.walioffice.deepseek.harness.domain.todo.TodoWriteTool;
import cn.walioffice.deepseek.harness.domain.todo.service.TodoWriteService;
import cn.walioffice.deepseek.harness.domain.llm.adapter.port.ILlmRuntimePort;
import cn.walioffice.deepseek.harness.domain.session.event.model.entity.SessionHeader;
import cn.walioffice.deepseek.harness.domain.session.event.model.entity.SessionLog;
import cn.walioffice.deepseek.harness.domain.tool.adapter.ToolRegistry;
import cn.walioffice.deepseek.harness.domain.tool.model.entity.ToolDefinition;
import cn.walioffice.deepseek.harness.domain.tool.fs.FsReadTool;
import cn.walioffice.deepseek.harness.domain.tool.fs.FsSearchTool;
import cn.walioffice.deepseek.harness.domain.tool.fs.FsWriteTool;
import cn.walioffice.deepseek.harness.domain.tool.fs.adapter.port.IFsPort;
import cn.walioffice.deepseek.harness.domain.tool.fs.StrReplaceEditorTool;
import cn.walioffice.deepseek.harness.domain.tool.service.ToolCallExecutor;
import cn.walioffice.deepseek.harness.domain.tool.service.MatrixRuntimeApprovalGate;
import cn.walioffice.deepseek.harness.domain.task.permission.adapter.port.IPermissionMatrixPort;
import cn.walioffice.deepseek.harness.domain.tool.shell.IShellExecutor;
import cn.walioffice.deepseek.harness.domain.tool.shell.ShellExecuteTool;
import cn.walioffice.deepseek.harness.domain.tool.web.adapter.port.IWebPort;
import cn.walioffice.deepseek.harness.domain.tool.web.WebFetchTool;
import cn.walioffice.deepseek.harness.domain.tool.web.WebSearchTool;
import cn.walioffice.deepseek.harness.infrastructure.adapter.tool.InMemoryToolRegistry;
import cn.walioffice.deepseek.harness.infrastructure.adapter.tool.JacksonToolArgumentsParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 智能体工厂与注册中心。
 * <p>
 * 执行流程：
 * 1. 创建新的智能体实例或恢复已有会话；
 * 2. 维护当前活跃智能体的注册表；
 * 3. 在应用关闭时统一释放资源。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class AgentFactory implements cn.walioffice.deepseek.harness.domain.agent.service.AgentFactoryPort {

    private final ILlmRuntimePort llm;
    private final ToolRegistry sharedToolRegistry;
    private final SystemPromptAssembler systemPromptAssembler;
    private final UserQuestionsPort userQuestionsPort;
    private final IShellExecutor shellExecutor;
    private final IFsPort fsService;
    private final IWebPort webService;
    private final PlanModeService planModeService;
    private final TodoWriteService todoWriteService;
    private final SubagentRegistry subagentRegistry;
    private final IHookService hookService;
    private final ObjectMapper objectMapper;
    private final IPermissionMatrixPort permissionMatrixPort;
    private final RuntimeApprovalBroker approvalBroker;
    private final ConcurrentMap<String, ReactLoopAgent> liveAgents = new ConcurrentHashMap<>();

    public AgentFactory(
            ILlmRuntimePort llm,
            ToolRegistry toolRegistry,
            SystemPromptAssembler systemPromptAssembler,
            UserQuestionsPort userQuestionsPort,
            IShellExecutor shellExecutor,
            IFsPort fsService,
            IWebPort webService,
            PlanModeService planModeService,
            TodoWriteService todoWriteService,
            SubagentRegistry subagentRegistry,
            IHookService hookService,
            ObjectMapper objectMapper
    ) {
        this(llm, toolRegistry, systemPromptAssembler, userQuestionsPort, shellExecutor,
                fsService, webService, planModeService, todoWriteService, subagentRegistry,
                hookService, objectMapper, null, null);
    }

    public AgentFactory(
            ILlmRuntimePort llm,
            ToolRegistry toolRegistry,
            SystemPromptAssembler systemPromptAssembler,
            UserQuestionsPort userQuestionsPort,
            IShellExecutor shellExecutor,
            IFsPort fsService,
            IWebPort webService,
            PlanModeService planModeService,
            TodoWriteService todoWriteService,
            SubagentRegistry subagentRegistry,
            IHookService hookService,
            ObjectMapper objectMapper,
            IPermissionMatrixPort permissionMatrixPort,
            RuntimeApprovalBroker approvalBroker
    ) {
        this.llm = llm;
        this.sharedToolRegistry = toolRegistry;
        this.systemPromptAssembler = systemPromptAssembler;
        this.userQuestionsPort = userQuestionsPort;
        this.shellExecutor = shellExecutor;
        this.fsService = fsService;
        this.webService = webService;
        this.planModeService = planModeService;
        this.todoWriteService = todoWriteService;
        this.subagentRegistry = subagentRegistry;
        this.hookService = hookService;
        this.objectMapper = objectMapper;
        this.permissionMatrixPort = permissionMatrixPort;
        this.approvalBroker = approvalBroker;
    }

    /**
     * 创建一个带新会话的智能体。
     * <p>
     * 执行流程：
     * 1. 初始化会话；
     * 2. 组装所需依赖；
     * 3. 注册并返回智能体实例。
     */
    public ReactLoopAgent create(String agentId, AgentOptions options, String cwd) {
        String sessionId = agentId + "-session-" + UUID.randomUUID();
        SessionHeader header = SessionHeader.create(sessionId, Instant.now(), cwd, agentId);
        SessionLog session = new SessionLog(sessionId, header);
        return wireAgent(agentId, options, cwd, session);
    }

    /**
     * 从已有会话日志恢复智能体。
     * <p>
     * 执行流程：
     * 1. 读取已有会话日志；
     * 2. 重建智能体运行上下文；
     * 3. 注册并返回恢复后的实例。
     */
    public ReactLoopAgent resume(String agentId, AgentOptions options, String cwd, SessionLog session) {
        return wireAgent(agentId, options, cwd, session);
    }

    private ReactLoopAgent wireAgent(String agentId, AgentOptions options, String cwd, SessionLog session) {
        Inbox inbox = new Inbox(session, LoggingInboxNotifications.INSTANCE);

        // 每个智能体独立的工具注册表，包含基础工具
        InMemoryToolRegistry agentTools = new InMemoryToolRegistry();
        agentTools.register(new TodoWriteTool(session, false, todoWriteService));
        agentTools.register(new AskUserTool(userQuestionsPort, agentId));
        java.util.concurrent.atomic.AtomicReference<PlanModeState> planState =
                new java.util.concurrent.atomic.AtomicReference<>(new PlanModeState.Off());
        agentTools.register(new ExitPlanModeTool(session, planState, planModeService));

        // 第二阶段：Shell / 文件系统 / 网页工具
        agentTools.register(new ShellExecuteTool(shellExecutor, cwd));
        agentTools.register(new FsReadTool(fsService));
        agentTools.register(new FsWriteTool(fsService));
        agentTools.register(new FsSearchTool(fsService));
        agentTools.register(new StrReplaceEditorTool(fsService));
        agentTools.register(new WebSearchTool(webService));
        agentTools.register(new WebFetchTool(webService));

        // 子代理工具：模型可通过 subagent 调用派生子智能体
        if (subagentRegistry != null && !subagentRegistry.listProviderNames().isEmpty()) {
            String firstProvider = subagentRegistry.listProviderNames().get(0);
            agentTools.register(new SubagentTool(subagentRegistry, firstProvider));
        }

        ToolRegistry toolView = new ToolRegistry() {
            @Override
            public Runnable register(ToolDefinition definition) {
                return agentTools.register(definition);
            }

            @Override
            public java.util.Optional<ToolDefinition> lookup(String name) {
                java.util.Optional<ToolDefinition> local = agentTools.lookup(name);
                return local.isPresent() ? local : sharedToolRegistry.lookup(name);
            }

            @Override
            public java.util.Collection<ToolDefinition> all() {
                java.util.Map<String, ToolDefinition> merged = new java.util.LinkedHashMap<>();
                sharedToolRegistry.all().forEach(tool -> merged.put(tool.name(), tool));
                agentTools.all().forEach(tool -> merged.put(tool.name(), tool));
                return merged.values();
            }

            @Override
            public cn.walioffice.deepseek.harness.domain.tool.model.valobj.ToolExecutionMode executionMode(
                    cn.walioffice.deepseek.harness.domain.tool.model.entity.ToolExecutionInput exec
            ) {
                ToolDefinition definition = lookup(exec.name()).orElse(null);
                if (definition == null) {
                    return cn.walioffice.deepseek.harness.domain.tool.model.valobj.ToolExecutionMode.EXCLUSIVE;
                }
                return definition.isConcurrencySafe(exec.arguments())
                        ? cn.walioffice.deepseek.harness.domain.tool.model.valobj.ToolExecutionMode.PARALLEL
                        : cn.walioffice.deepseek.harness.domain.tool.model.valobj.ToolExecutionMode.EXCLUSIVE;
            }
        };

        ToolCallExecutor executor = new ToolCallExecutor(
                toolView,
                session,
                10,
                new JacksonToolArgumentsParser(objectMapper)
        );
        if (hookService != null) {
            executor.setHookService(hookService);
        }

        // 运行期审批门禁：当权限矩阵与审批 Broker 均就绪时接入，
        // agent 执行到需审批工具会经 broker 挂起等待人工裁决
        if (permissionMatrixPort != null && approvalBroker != null) {
            MatrixRuntimeApprovalGate gate = new MatrixRuntimeApprovalGate(
                    permissionMatrixPort,
                    session.sessionId(),
                    (req, g) -> approvalBroker.requestApproval(req)
            );
            executor.setApprovalGate(gate);
        }

        ReactLoopAgent agent = new ReactLoopAgent(
                agentId, options, cwd, session, inbox,
                llm, agentTools, executor,
                systemPromptAssembler, null
        );
        liveAgents.put(agentId, agent);
        return agent;
    }

    /**
     * 按编号查找活跃智能体。
     * <p>
     * 执行流程：
     * 1. 读取 agentId；
     * 2. 在注册表中查找；
     * 3. 返回对应实例。
     */
    public ReactLoopAgent get(String agentId) {
        return liveAgents.get(agentId);
    }

    /**
     * 获取全部活跃智能体。
     * <p>
     * 执行流程：
     * 1. 遍历注册表；
     * 2. 收集当前存活实例；
     * 3. 返回列表。
     */
    public ConcurrentMap<String, ReactLoopAgent> all() {
        return liveAgents;
    }

    /**
     * 释放全部活跃智能体。
     * <p>
     * 执行流程：
     * 1. 遍历所有注册实例；
     * 2. 逐个发起释放；
     * 3. 完成后清空注册表。
     */
    public java.util.concurrent.CompletableFuture<Void> disposeAll() {
        return java.util.concurrent.CompletableFuture.allOf(
                liveAgents.values().stream()
                        .map(ReactLoopAgent::dispose)
                        .toArray(java.util.concurrent.CompletableFuture[]::new)
        );
    }
}
