package cn.xiaofuge.deepseek.harness.runtime.agent;

import cn.xiaofuge.deepseek.harness.domain.agent.ask.AskUserTool;
import cn.xiaofuge.deepseek.harness.domain.agent.adapter.port.IRuntimeApprovalBroker;
import cn.xiaofuge.deepseek.harness.domain.agent.ask.UserQuestionsPort;
import cn.xiaofuge.deepseek.harness.domain.agent.model.entity.AgentOptions;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolExecutionInput;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.ToolExecutionMode;
import cn.xiaofuge.deepseek.harness.domain.plan.ExitPlanModeTool;
import cn.xiaofuge.deepseek.harness.domain.agent.service.*;
import cn.xiaofuge.deepseek.harness.domain.plan.model.valobj.PlanModeState;
import cn.xiaofuge.deepseek.harness.domain.plan.service.PlanModeService;
import cn.xiaofuge.deepseek.harness.domain.agent.subagent.SubagentRegistry;
import cn.xiaofuge.deepseek.harness.domain.agent.subagent.SubagentTool;
import cn.xiaofuge.deepseek.harness.domain.hooks.IHookService;
import cn.xiaofuge.deepseek.harness.domain.todo.service.TodoWriteTool;
import cn.xiaofuge.deepseek.harness.domain.todo.service.TodoWriteService;
import cn.xiaofuge.deepseek.harness.domain.llm.adapter.port.ILlmRuntimePort;
import cn.xiaofuge.deepseek.harness.domain.session.event.adapter.repository.ISessionEventStore;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionHeader;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionLog;
import cn.xiaofuge.deepseek.harness.domain.tool.adapter.ToolRegistry;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolDefinition;
import cn.xiaofuge.deepseek.harness.domain.tool.fs.FsReadTool;
import cn.xiaofuge.deepseek.harness.domain.tool.fs.FsSearchTool;
import cn.xiaofuge.deepseek.harness.domain.tool.fs.FsWriteTool;
import cn.xiaofuge.deepseek.harness.domain.tool.fs.adapter.port.IFsPort;
import cn.xiaofuge.deepseek.harness.domain.tool.fs.StrReplaceEditorTool;
import cn.xiaofuge.deepseek.harness.domain.tool.service.ToolCallExecutor;
import cn.xiaofuge.deepseek.harness.domain.tool.service.MatrixRuntimeApprovalGate;
import cn.xiaofuge.deepseek.harness.domain.task.permission.adapter.port.IPermissionMatrixPort;
import cn.xiaofuge.deepseek.harness.domain.tool.shell.IShellExecutor;
import cn.xiaofuge.deepseek.harness.domain.tool.shell.ShellExecuteTool;
import cn.xiaofuge.deepseek.harness.domain.tool.web.adapter.port.IWebPort;
import cn.xiaofuge.deepseek.harness.domain.tool.web.WebFetchTool;
import cn.xiaofuge.deepseek.harness.domain.tool.web.WebSearchTool;
import cn.xiaofuge.deepseek.harness.runtime.tool.InMemoryToolRegistry;
import cn.xiaofuge.deepseek.harness.runtime.tool.JacksonToolArgumentsParser;
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
 * <p>调用流程：领域端口 → 基础设施适配器 → 外部系统。</p>
 * <p>示例：适配器只实现端口契约，业务规则保留在 domain。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public class AgentFactory implements AgentFactoryPort {

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
    private final IRuntimeApprovalBroker approvalBroker;
    private final ISessionEventStore sessionEventStore;
    private final ConcurrentMap<String, ReactLoopAgent> liveAgents = new ConcurrentHashMap<>();

    /**
     * 初始化默认 Agent 工厂。
     * <p>流程：装配共享依赖 → 缓存 Agent 实例 → 支持创建/查找/取消。</p>
     */
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
                hookService, objectMapper, null, null, null);
    }

    /**
     * 初始化带自定义运行时的 Agent 工厂。
     * <p>流程：装配共享依赖 → 注册自定义 LLM 端口 → 缓存 Agent 实例。</p>
     */
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
            IRuntimeApprovalBroker approvalBroker
    ) {
        this(llm, toolRegistry, systemPromptAssembler, userQuestionsPort, shellExecutor,
                fsService, webService, planModeService, todoWriteService, subagentRegistry,
                hookService, objectMapper, permissionMatrixPort, approvalBroker, null);
    }

    /**
     * 全参构造器——同时接入运行期审批门禁和事件溯源持久化。
     * <p>
     * 当 sessionEventStore 非空时，Agent 的会话事件会被镜像写入持久化存储，
     * Agent 内部事件（emit/serial）也会被桥接记录，让事件溯源设施真正运转。
     */
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
            IRuntimeApprovalBroker approvalBroker,
            ISessionEventStore sessionEventStore
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
        this.sessionEventStore = sessionEventStore;
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
        String sessionId = UUID.randomUUID().toString();
        SessionHeader header = SessionHeader.create(sessionId, Instant.now(), cwd, agentId);
        SessionLog session = sessionEventStore != null
                ? new PersistingSessionLog(sessionId, header, sessionEventStore)
                : new SessionLog(sessionId, header);
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

    /**
     * 处理wire Agent。
     * 流程：适配外部协议或资源 → 调用底层能力 → 返回结果。
     * @param agentId Agent ID。
     * @param options Agent 配置。
     * @param cwd 工作目录。
     * @param session 会话参数。
     * @return 方法执行结果。
     */
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

            /**
             * register。
             * 流程：适配外部协议或资源 → 调用底层能力 → 返回结果。
             * @param definition definition参数。
             * @return 方法执行结果。
             */

        ToolRegistry toolView = new ToolRegistry() {
            @Override
            public Runnable register(ToolDefinition definition) {
                return agentTools.register(definition);
            }
            /**
             * lookup。
             * 流程：适配外部协议或资源 → 调用底层能力 → 返回结果。
             * @param name 名称。
             * @return 方法执行结果。
             */


            @Override
            public java.util.Optional<ToolDefinition> lookup(String name) {
                java.util.Optional<ToolDefinition> local = agentTools.lookup(name);
                return local.isPresent() ? local : sharedToolRegistry.lookup(name);
            }
            /**
             * all。
             * 流程：适配外部协议或资源 → 调用底层能力 → 返回结果。
             * @return 方法执行结果。
             */


            @Override
            public java.util.Collection<ToolDefinition> all() {
                java.util.Map<String, ToolDefinition> merged = new java.util.LinkedHashMap<>();
                sharedToolRegistry.all().forEach(tool -> merged.put(tool.name(), tool));
                agentTools.all().forEach(tool -> merged.put(tool.name(), tool));
                return merged.values();
            }
            /**
             * execution Mode。
             * 流程：适配外部协议或资源 → 调用底层能力 → 返回结果。
             * @param exec exec参数。
             * @return 方法执行结果。
             */


            @Override
            public ToolExecutionMode executionMode(
                    ToolExecutionInput exec
            ) {
                ToolDefinition definition = lookup(exec.name()).orElse(null);
                if (definition == null) {
                    return ToolExecutionMode.EXCLUSIVE;
                }
                return definition.isConcurrencySafe(exec.arguments())
                        ? ToolExecutionMode.PARALLEL
                        : ToolExecutionMode.EXCLUSIVE;
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

        // Agent 事件监听器：当事件溯源存储就绪时，桥接 Agent 内部事件
        // （emit/serial/waterfall）到持久化存储，让事件溯源覆盖 Agent 生命周期。
        AgentEventListener agentEvents = sessionEventStore != null
                ? new PersistingAgentEventListener(session.sessionId(), sessionEventStore, objectMapper)
                : null;

        ReactLoopAgent agent = new ReactLoopAgent(
                agentId, options, cwd, session, inbox,
                llm, toolView, executor,
                systemPromptAssembler, agentEvents
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
