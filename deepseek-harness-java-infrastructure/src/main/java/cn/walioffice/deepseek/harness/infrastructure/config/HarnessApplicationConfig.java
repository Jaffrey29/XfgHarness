package cn.walioffice.deepseek.harness.infrastructure.config;

import cn.walioffice.deepseek.harness.domain.task.approval.adapter.port.IApprovalPolicyPort;
import cn.walioffice.deepseek.harness.domain.task.approval.service.ApprovalCommandService;
import cn.walioffice.deepseek.harness.domain.task.approval.service.ApprovalQueryService;
import cn.walioffice.deepseek.harness.domain.task.IApprovalCommandService;
import cn.walioffice.deepseek.harness.domain.task.IApprovalQueryService;
import cn.walioffice.deepseek.harness.domain.task.approval.service.ApprovalPolicyService;
import cn.walioffice.deepseek.harness.domain.task.IApprovalPolicyService;
import cn.walioffice.deepseek.harness.domain.session.conversation.service.ConversationQueryService;
import cn.walioffice.deepseek.harness.domain.session.IConversationQueryService;
import cn.walioffice.deepseek.harness.domain.task.execution.adapter.port.IModelExecutionPort;
import cn.walioffice.deepseek.harness.domain.task.execution.service.HarnessExecutionService;
import cn.walioffice.deepseek.harness.domain.task.IHarnessExecutionService;
import cn.walioffice.deepseek.harness.domain.runtime.model.adapter.port.IModelProviderPort;
import cn.walioffice.deepseek.harness.domain.runtime.IModelRoutingService;
import cn.walioffice.deepseek.harness.domain.runtime.model.service.ModelRoutingService;
import cn.walioffice.deepseek.harness.domain.runtime.setting.adapter.repository.IModelSettingRepository;
import cn.walioffice.deepseek.harness.domain.runtime.setting.service.ModelSettingService;
import cn.walioffice.deepseek.harness.domain.plugin.bridge.adapter.port.IPluginRuntimeBridgePort;
import cn.walioffice.deepseek.harness.domain.plugin.bridge.adapter.repository.IHarnessPluginRuntimeBindingRepository;
import cn.walioffice.deepseek.harness.domain.plugin.IPluginBridgeService;
import cn.walioffice.deepseek.harness.domain.plugin.bridge.service.PluginBridgeService;
import cn.walioffice.deepseek.harness.domain.plugin.registry.adapter.port.IPluginArtifactInstallerPort;
import cn.walioffice.deepseek.harness.domain.plugin.registry.adapter.repository.IHarnessPluginRepository;
import cn.walioffice.deepseek.harness.domain.plugin.IPluginRegistryService;
import cn.walioffice.deepseek.harness.domain.plugin.registry.service.PluginRegistryService;
import cn.walioffice.deepseek.harness.domain.plugin.runtime.adapter.port.IPluginProcessPort;
import cn.walioffice.deepseek.harness.domain.plugin.IPluginRuntimeService;
import cn.walioffice.deepseek.harness.domain.plugin.runtime.service.PluginRuntimeService;
import cn.walioffice.deepseek.harness.domain.task.permission.adapter.port.IPermissionMatrixPort;
import cn.walioffice.deepseek.harness.domain.task.IPermissionPolicyService;
import cn.walioffice.deepseek.harness.domain.task.permission.service.PermissionPolicyService;
import cn.walioffice.deepseek.harness.domain.runtime.profile.service.ExecutionProfileService;
import cn.walioffice.deepseek.harness.domain.runtime.IExecutionProfileService;
import cn.walioffice.deepseek.harness.domain.runtime.profile.service.router.ExecutionProfileStrategy;
import cn.walioffice.deepseek.harness.domain.runtime.profile.service.router.HeadlessExecutionProfileStrategy;
import cn.walioffice.deepseek.harness.domain.runtime.profile.service.router.WebExecutionProfileStrategy;
import cn.walioffice.deepseek.harness.domain.session.event.adapter.repository.ISessionEventStore;
import cn.walioffice.deepseek.harness.domain.session.event.service.ISessionEventLogService;
import cn.walioffice.deepseek.harness.domain.session.event.service.LegacyEventAdapter;
import cn.walioffice.deepseek.harness.domain.session.event.service.SessionEventLogService;
import cn.walioffice.deepseek.harness.domain.session.event.service.SessionRebuilderService;
import cn.walioffice.deepseek.harness.domain.session.log.adapter.repository.ISessionEventRepository;
import cn.walioffice.deepseek.harness.domain.session.ISessionLogService;
import cn.walioffice.deepseek.harness.domain.session.log.service.SessionLogService;
import cn.walioffice.deepseek.harness.domain.session.registry.adapter.repository.IHarnessSessionRepository;
import cn.walioffice.deepseek.harness.domain.session.registry.service.HarnessSessionRegistryService;
import cn.walioffice.deepseek.harness.domain.session.IHarnessSessionRegistryService;
import cn.walioffice.deepseek.harness.domain.task.ITaskSubmissionPolicyService;
import cn.walioffice.deepseek.harness.domain.task.submission.service.TaskSubmissionPolicyService;
import cn.walioffice.deepseek.harness.domain.task.submission.service.filter.ProfileCodeFilter;
import cn.walioffice.deepseek.harness.domain.task.submission.service.filter.PromptNotBlankFilter;
import cn.walioffice.deepseek.harness.domain.task.submission.service.filter.TaskSubmissionFilter;
import cn.walioffice.deepseek.harness.domain.task.submission.service.filter.ToolSelectionFilter;
import cn.walioffice.deepseek.harness.domain.goal.service.GoalService;
import cn.walioffice.deepseek.harness.domain.goal.IGoalCommandService;
import cn.walioffice.deepseek.harness.domain.goal.IGoalQueryService;
import cn.walioffice.deepseek.harness.domain.plan.service.PlanModeService;
import cn.walioffice.deepseek.harness.domain.todo.service.TodoWriteService;
import cn.walioffice.deepseek.harness.domain.terminal.ITerminalCommandService;
import cn.walioffice.deepseek.harness.domain.terminal.ITerminalQueryService;
import cn.walioffice.deepseek.harness.domain.terminal.adapter.port.ITerminalPort;
import cn.walioffice.deepseek.harness.domain.terminal.service.TerminalService;
import cn.walioffice.deepseek.harness.domain.task.queue.adapter.repository.IHarnessTaskQueueRepository;
import cn.walioffice.deepseek.harness.domain.task.queue.service.HarnessTaskQueueService;
import cn.walioffice.deepseek.harness.domain.task.IHarnessTaskQueueService;
import cn.walioffice.deepseek.harness.domain.runtime.tool.adapter.port.IToolCatalogPort;
import cn.walioffice.deepseek.harness.domain.runtime.IToolResolutionService;
import cn.walioffice.deepseek.harness.domain.runtime.tool.service.ToolResolutionService;
import cn.walioffice.deepseek.harness.domain.llm.adapter.LlmAdapter;
import cn.walioffice.deepseek.harness.domain.llm.adapter.ResolvedRetryPolicy;
import cn.walioffice.deepseek.harness.domain.llm.adapter.port.ILlmRuntimePort;
import cn.walioffice.deepseek.harness.domain.llm.service.LlmGenerationService;
import cn.walioffice.deepseek.harness.domain.agent.service.SystemPromptAssembler;
import cn.walioffice.deepseek.harness.domain.agent.service.ScopedSystemPromptAssembler;
import cn.walioffice.deepseek.harness.domain.agent.compaction.BasicCompactionEngine;
import cn.walioffice.deepseek.harness.domain.agent.compaction.CompactionEngine;
import cn.walioffice.deepseek.harness.domain.agent.subagent.SubagentRegistry;
import cn.walioffice.deepseek.harness.domain.tool.adapter.ToolRegistry;
import cn.walioffice.deepseek.harness.domain.tool.fs.adapter.port.IFsPort;
import cn.walioffice.deepseek.harness.domain.tool.shell.IShellExecutor;
import cn.walioffice.deepseek.harness.domain.tool.web.adapter.port.IWebPort;
import cn.walioffice.deepseek.harness.infrastructure.adapter.agent.AgentFactory;
import cn.walioffice.deepseek.harness.infrastructure.adapter.agent.InMemoryUserQuestionsPort;
import cn.walioffice.deepseek.harness.infrastructure.adapter.agent.RuntimeApprovalBroker;
import cn.walioffice.deepseek.harness.infrastructure.adapter.fs.LocalFsService;
import cn.walioffice.deepseek.harness.infrastructure.adapter.fs.SandboxedFsService;
import cn.walioffice.deepseek.harness.infrastructure.adapter.llm.InMemoryLlmRuntimePort;
import cn.walioffice.deepseek.harness.infrastructure.adapter.llm.openai.OpenAiCompatibleAdapter;
import cn.walioffice.deepseek.harness.infrastructure.adapter.llm.deepseek.DeepSeekAdapter;
import cn.walioffice.deepseek.harness.infrastructure.adapter.llm.deepseek.DeepSeekAdapterOptions;
import cn.walioffice.deepseek.harness.infrastructure.adapter.llm.deepseek.DirectSeekCatalogModel;
import cn.walioffice.deepseek.harness.infrastructure.adapter.mcp.StdioMcpClient;
import cn.walioffice.deepseek.harness.infrastructure.adapter.sandbox.SandboxedShellExecutor;
import cn.walioffice.deepseek.harness.infrastructure.adapter.shell.LocalShellExecutor;
import cn.walioffice.deepseek.harness.infrastructure.adapter.tool.InMemoryToolRegistry;
import cn.walioffice.deepseek.harness.infrastructure.adapter.web.LocalWebService;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.walioffice.deepseek.harness.domain.sdk.ISdkClientService;
import cn.walioffice.deepseek.harness.domain.sdk.ISdkServerService;
import cn.walioffice.deepseek.harness.domain.typert.adapter.port.ITypertRegistryContract;
import cn.walioffice.deepseek.harness.domain.sdk.service.SdkClientService;
import cn.walioffice.deepseek.harness.domain.sdk.service.SdkServerService;
import cn.walioffice.deepseek.harness.domain.typert.ITypertService;
import cn.walioffice.deepseek.harness.domain.typert.service.TypertService;
import cn.walioffice.deepseek.harness.infrastructure.adapter.sdk.StdioSdkTransportPort;
import cn.walioffice.deepseek.harness.infrastructure.adapter.typert.InMemoryTypertRegistry;
import cn.walioffice.deepseek.harness.infrastructure.adapter.typert.TypertProtocolLoader;

import cn.walioffice.deepseek.harness.infrastructure.adapter.e2b.StubE2BFsPort;
import cn.walioffice.deepseek.harness.infrastructure.adapter.e2b.StubE2BSubprocessPort;
import cn.walioffice.deepseek.harness.infrastructure.adapter.acp.StubAcpAgentPort;

import cn.walioffice.deepseek.harness.domain.workflow.IWorkflowService;
import cn.walioffice.deepseek.harness.domain.workflow.adapter.port.IWorkflowEnginePort;
import cn.walioffice.deepseek.harness.domain.workflow.service.WorkflowService;
import cn.walioffice.deepseek.harness.infrastructure.adapter.workflow.LocalWorkflowEnginePort;

import cn.walioffice.deepseek.harness.domain.acp.IAcpServerService;
import cn.walioffice.deepseek.harness.domain.acp.adapter.port.IAcpAgentPort;
import cn.walioffice.deepseek.harness.domain.acp.service.AcpServerService;
import cn.walioffice.deepseek.harness.domain.coderuntime.ICodeRuntimeService;
import cn.walioffice.deepseek.harness.domain.coderuntime.adapter.port.ICodeRuntimePort;
import cn.walioffice.deepseek.harness.domain.coderuntime.service.CodeRuntimeService;
import cn.walioffice.deepseek.harness.domain.e2b.IE2BSandboxService;
import cn.walioffice.deepseek.harness.domain.e2b.adapter.port.IE2BFsPort;
import cn.walioffice.deepseek.harness.domain.e2b.adapter.port.IE2BSubprocessPort;
import cn.walioffice.deepseek.harness.domain.e2b.service.E2BSandboxService;
import cn.walioffice.deepseek.harness.domain.lsp.ILspService;
import cn.walioffice.deepseek.harness.domain.lsp.service.LspService;
import cn.walioffice.deepseek.harness.infrastructure.adapter.acp.StdioAcpAgentPort;
import cn.walioffice.deepseek.harness.infrastructure.adapter.coderuntime.LocalCodeRuntimePort;
import cn.walioffice.deepseek.harness.infrastructure.adapter.subagent.AcpSubagentProvider;
import cn.walioffice.deepseek.harness.infrastructure.adapter.subagent.ClaudeCodeSubagentProvider;
import cn.walioffice.deepseek.harness.infrastructure.adapter.subagent.CodexSubagentProvider;
import cn.walioffice.deepseek.harness.infrastructure.adapter.terminal.LocalTerminalPort;

import cn.walioffice.deepseek.harness.domain.skill.ISkillService;
import cn.walioffice.deepseek.harness.domain.skill.adapter.port.ISkillProviderPort;
import cn.walioffice.deepseek.harness.domain.skill.service.SkillService;
import cn.walioffice.deepseek.harness.infrastructure.adapter.skill.FilesystemSkillProviderPort;

import cn.walioffice.deepseek.harness.domain.hooks.IHookService;
import cn.walioffice.deepseek.harness.domain.hooks.adapter.port.IHookRunnerPort;
import cn.walioffice.deepseek.harness.domain.hooks.service.HookService;
import cn.walioffice.deepseek.harness.infrastructure.adapter.hooks.LocalShellHookRunner;

import cn.walioffice.deepseek.harness.domain.jobs.IJobRegistryService;
import cn.walioffice.deepseek.harness.domain.jobs.adapter.repository.IJobRegistryRepository;
import cn.walioffice.deepseek.harness.domain.jobs.service.JobRegistryService;
import cn.walioffice.deepseek.harness.infrastructure.adapter.jobs.InMemoryJobRegistryRepository;

import cn.walioffice.deepseek.harness.domain.schedule.IScheduleService;
import cn.walioffice.deepseek.harness.domain.schedule.adapter.repository.IScheduleRepository;
import cn.walioffice.deepseek.harness.domain.schedule.service.ScheduleService;
import cn.walioffice.deepseek.harness.infrastructure.adapter.schedule.InMemoryScheduleRepository;

import cn.walioffice.deepseek.harness.domain.sandbox.ISandboxPolicyService;
import cn.walioffice.deepseek.harness.domain.sandbox.model.valobj.SandboxMode;
import cn.walioffice.deepseek.harness.domain.sandbox.service.SandboxPolicyService;

import cn.walioffice.deepseek.harness.domain.guard.IGuardService;
import cn.walioffice.deepseek.harness.domain.guard.service.GuardService;

import cn.walioffice.deepseek.harness.domain.storage.IStorageService;
import cn.walioffice.deepseek.harness.domain.storage.adapter.port.IStorageBackendPort;
import cn.walioffice.deepseek.harness.domain.storage.service.StorageService;
import cn.walioffice.deepseek.harness.infrastructure.adapter.storage.JsonStorageBackendPort;
import cn.walioffice.deepseek.harness.domain.tool.plugin.IPluginToolBridge;
import cn.walioffice.deepseek.harness.domain.tool.plugin.PluginToolBridgeService;
import cn.walioffice.deepseek.harness.infrastructure.adapter.plugin.JsonRpcPluginToolBridge;
import cn.walioffice.deepseek.harness.infrastructure.adapter.tool.JacksonToolArgumentsParser;
import cn.walioffice.deepseek.harness.domain.tool.service.ToolArgumentsParser;
import java.nio.file.Path;

import cn.walioffice.deepseek.harness.domain.credentials.ICredentialService;
import cn.walioffice.deepseek.harness.domain.credentials.adapter.port.ICredentialProviderPort;
import cn.walioffice.deepseek.harness.domain.credentials.service.CredentialService;
import cn.walioffice.deepseek.harness.infrastructure.adapter.credentials.LocalCredentialProviderPort;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 基础设施装配配置。
 * <p>
 * 执行流程：
 * 1. 将领域层接口与基础设施实现绑定；
 * 2. 按功能域装配各类 Bean；
 * 3. 为触发层和用例层提供统一注入入口。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Configuration
public class HarnessApplicationConfig {

    @Bean
    public IPluginRegistryService pluginRegistryService(
            IHarnessPluginRepository harnessPluginRepository,
            IPluginArtifactInstallerPort pluginArtifactInstallerPort
    ) {
        return new PluginRegistryService(harnessPluginRepository, pluginArtifactInstallerPort);
    }

    @Bean
    public IPluginBridgeService pluginBridgeService(
            IPluginRuntimeBridgePort pluginRuntimeBridgePort,
            IHarnessPluginRuntimeBindingRepository harnessPluginRuntimeBindingRepository
    ) {
        return new PluginBridgeService(pluginRuntimeBridgePort, harnessPluginRuntimeBindingRepository);
    }

    @Bean
    public IPluginRuntimeService pluginRuntimeService(IPluginProcessPort pluginProcessPort) {
        return new PluginRuntimeService(pluginProcessPort);
    }

    @Bean
    public IPluginToolBridge jsonRpcPluginToolBridge() {
        return new JsonRpcPluginToolBridge();
    }

    @Bean
    public PluginToolBridgeService pluginToolBridgeService(
            ToolRegistry toolRegistry,
            IPluginToolBridge pluginToolBridge,
            SystemPromptAssembler systemPromptAssembler
    ) {
        return new PluginToolBridgeService(toolRegistry, pluginToolBridge, systemPromptAssembler);
    }

    @Bean
    public ToolArgumentsParser toolArgumentsParser(ObjectMapper objectMapper) {
        return new JacksonToolArgumentsParser(objectMapper);
    }

    @Bean
    public ITaskSubmissionPolicyService taskSubmissionPolicyService() {
        List<TaskSubmissionFilter> filters = List.of(
                new PromptNotBlankFilter(),
                new ProfileCodeFilter(),
                new ToolSelectionFilter()
        );
        return new TaskSubmissionPolicyService(filters);
    }

    /**
     * 创建执行画像服务。
     * <p>
     * 执行流程：
     * 1. 组装画像策略列表；
     * 2. 创建领域服务；
     * 3. 返回 Spring Bean。
     */
    @Bean
    public IExecutionProfileService executionProfileService() {
        List<ExecutionProfileStrategy> strategies = List.of(
                new HeadlessExecutionProfileStrategy(),
                new WebExecutionProfileStrategy()
        );
        return new ExecutionProfileService(strategies);
    }

    /**
     * 创建模型路由服务。
     * <p>
     * 执行流程：
     * 1. 注入模型提供方端口；
     * 2. 创建领域服务；
     * 3. 返回 Spring Bean。
     */
    @Bean
    public IModelRoutingService modelRoutingService(IModelProviderPort modelProviderPort) {
        return new ModelRoutingService(modelProviderPort);
    }

    /**
     * 创建工具解析服务。
     * <p>
     * 执行流程：
     * 1. 注入工具目录端口；
     * 2. 创建领域服务；
     * 3. 返回 Spring Bean。
     */
    @Bean
    public IToolResolutionService toolResolutionService(IToolCatalogPort toolCatalogPort) {
        return new ToolResolutionService(toolCatalogPort);
    }

    @Bean
    public ModelSettingService modelSettingService(IModelSettingRepository modelSettingRepository) {
        return new ModelSettingService(modelSettingRepository);
    }

    @Bean
    public IConversationQueryService conversationQueryService(
            IHarnessSessionRepository harnessSessionRepository,
            ISessionEventRepository sessionEventRepository,
            cn.walioffice.deepseek.harness.domain.session.event.adapter.repository.ISessionEventStore sessionEventStore
    ) {
        return new ConversationQueryService(harnessSessionRepository, sessionEventRepository, sessionEventStore);
    }

    @Bean
    public IHarnessExecutionService harnessExecutionService(
            IModelExecutionPort modelExecutionPort,
            IHarnessSessionRepository harnessSessionRepository,
            ISessionEventRepository sessionEventRepository,
            ISessionLogService sessionLogService
    ) {
        return new HarnessExecutionService(modelExecutionPort, harnessSessionRepository, sessionEventRepository, sessionLogService);
    }

    @Bean
    public IPermissionPolicyService permissionPolicyService(IPermissionMatrixPort permissionMatrixPort) {
        return new PermissionPolicyService(permissionMatrixPort);
    }

    @Bean
    public IApprovalPolicyService approvalPolicyService(IApprovalPolicyPort approvalPolicyPort) {
        return new ApprovalPolicyService(approvalPolicyPort);
    }

    @Bean
    public IApprovalQueryService approvalQueryService(
            IConversationQueryService conversationQueryService
    ) {
        return new ApprovalQueryService(conversationQueryService);
    }

    @Bean
    public IApprovalCommandService approvalCommandService(
            IHarnessSessionRepository harnessSessionRepository,
            ISessionLogService sessionLogService,
            IHarnessExecutionService harnessExecutionService
    ) {
        return new ApprovalCommandService(harnessSessionRepository, sessionLogService, harnessExecutionService);
    }

    @Bean
    public IHarnessSessionRegistryService harnessSessionRegistryService(IHarnessSessionRepository harnessSessionRepository) {
        return new HarnessSessionRegistryService(harnessSessionRepository);
    }

    @Bean
    public ISessionLogService sessionLogService(ISessionEventRepository sessionEventRepository) {
        return new SessionLogService(sessionEventRepository);
    }

    /**
     * 会话事件系统 Bean。
     */
    @Bean
    public ISessionEventLogService sessionEventLogService(ISessionEventStore sessionEventStore) {
        return new SessionEventLogService(sessionEventStore);
    }

    @Bean
    public LegacyEventAdapter legacyEventAdapter(ISessionEventLogService sessionEventLogService) {
        return new LegacyEventAdapter(sessionEventLogService);
    }

    @Bean
    public SessionRebuilderService sessionRebuilderService(ISessionEventLogService sessionEventLogService) {
        return new SessionRebuilderService(sessionEventLogService);
    }

    @Bean
    public IHarnessTaskQueueService harnessTaskQueueService(IHarnessTaskQueueRepository harnessTaskQueueRepository) {
        return new HarnessTaskQueueService(harnessTaskQueueRepository);
    }

    // ── 大模型层 Bean ───────────────────────────────────────

    @Bean
    public ILlmRuntimePort llmRuntimePort(List<LlmAdapter> adapterList) {
        InMemoryLlmRuntimePort runtime = new InMemoryLlmRuntimePort();
        for (LlmAdapter adapter : adapterList) {
            String providerId = adapter.providerId();
            runtime.registerAdapter(providerId, adapter);
        }
        return runtime;
    }

    @Bean
    public DeepSeekAdapter deepSeekAdapter(
            ObjectMapper objectMapper,
            @Value("${harness.llm.deepseek.base-url:http://127.0.0.1:8777/v1}") String baseUrl,
            @Value("${harness.llm.deepseek.api-key:sk-local}") String apiKey,
            @Value("${harness.llm.deepseek.max-tokens:8192}") int maxTokens,
            @Value("${harness.llm.deepseek.context-window:128000}") int contextWindow,
            @Value("${harness.llm.deepseek.default-model:deepseek}") String defaultModel
    ) {
        var models = List.of(
                new DirectSeekCatalogModel(defaultModel, defaultModel, null, contextWindow, maxTokens)
        );
        ResolvedRetryPolicy retryPolicy = new ResolvedRetryPolicy.Normal(
                500, 10_000, 0.1, 2,
                cn.walioffice.deepseek.harness.domain.llm.model.valobj.LlmErrorCodes.DEFAULT_RETRYABLE_CODES
        );
        var options = new DeepSeekAdapterOptions(baseUrl, apiKey, maxTokens, contextWindow,
                DeepSeekAdapter.DEFAULT_STREAM_IDLE_TIMEOUT_MS, models, retryPolicy);
        return new DeepSeekAdapter(options, objectMapper);
    }

    @Bean
    public OpenAiCompatibleAdapter openaiAdapter(ObjectMapper objectMapper) {
        var options = new DeepSeekAdapterOptions(
                "https://api.openai.com/v1",
                System.getenv("OPENAI_API_KEY") != null ? System.getenv("OPENAI_API_KEY") : "",
                4096, 128000, DeepSeekAdapter.DEFAULT_STREAM_IDLE_TIMEOUT_MS,
                List.of(), null);
        return new OpenAiCompatibleAdapter("openai", options, objectMapper);
    }

    @Bean
    public OpenAiCompatibleAdapter ollamaAdapter(ObjectMapper objectMapper) {
        var options = new DeepSeekAdapterOptions(
                "http://127.0.0.1:11434/v1",
                "ollama",
                4096, 128000, DeepSeekAdapter.DEFAULT_STREAM_IDLE_TIMEOUT_MS,
                List.of(), null);
        return new OpenAiCompatibleAdapter("ollama", options, objectMapper);
    }

    // ── 智能体层 Bean ─────────────────────────────────────

    @Bean
    public ToolRegistry toolRegistry() {
        return new InMemoryToolRegistry();
    }

    @Bean
    public SystemPromptAssembler systemPromptAssembler() {
        String identity = "You are DeepSeek Harness, an intelligent AI coding and task assistant.";
        String persona = """
                ## 角色
                你是一名专业、高效的 AI 助手，擅长编程、技术问答、文件操作和任务执行。
                默认使用中文回答，除非用户使用英文提问。

                ## 回答规范
                - 代码块必须标注语言（```java、```python 等）。
                - 默认回答 2-5 句，先给结论再展开细节；不要只用一句话草草收尾。
                - 复杂任务分步骤执行，每步说明意图，必要时给出下一步建议。
                - 不确定时主动澄清，不要猜测。

                ## 工具使用（重要）
                - 你处在一个可以调用真实工具的运行环境中。工具以 function calling 形式提供，必须真正发起工具调用才能获得结果。
                - 当任务涉及「读取/列出/搜索/写入文件、执行命令、查看目录、访问网页、查询代码」等任何需要真实数据的操作时，你必须调用对应工具，严禁凭空编造或复述请求。
                - 即使用户只说一句"列出当前目录的文件"，也必须调用工具获取真实结果，而不是回答"我来列出"。
                - **列出目录内容（一级子目录/文件）时，优先用 `shell_execute` 执行 `ls -la <目录>` 或 `find <目录> -maxdepth 1 -type d`；`fs_search` 是 glob 文件搜索工具，不适合列目录。**
                - 工具调用前简要说明目的，调用后基于真实结果解读。
                - 危险操作（删除、覆盖）前确认。
                - 任务完成后停止调用工具，给出最终回复。
                - 除非用户明确要求禁用工具，否则根据任务需要自主选择工具。
                - 工具失败时不要编造结果：说明失败原因，尝试更安全的替代路径或请求澄清。

                ## 工具结果解读（严格遵循）
                - 工具返回的就是真实数据，直接原样引用，不要二次加工或"翻译"。
                - 列出文件/目录时，用简洁的 Markdown 列表或代码块展示完整结果，不要截断、不要省略、不要说"还有很多"。
                - 如果工具结果很长，先总结关键信息，再用折叠块（```text ... ```）附完整结果。
                - 严禁出现"项目内部文件占满""无法完整体现"这类模糊表述——工具返回了什么就展示什么。
                - 最终回复中不要包含反引号不匹配的片段、乱码或未完成的句子。
                - 每次回复必须是完整、自洽的一段话，检查无误后再输出。

                ## 格式
                - 使用 Markdown 格式化输出。
                - 长回答用标题和列表组织结构。
                - 思考过程用 <think></think> 包裹。
                """;
        var assembler = new ScopedSystemPromptAssembler(identity, persona, java.util.List.of());
        assembler.registerContext(new ScopedSystemPromptAssembler.PromptContext(
                "harness:runtime",
                20,
                (java.util.function.Function<ScopedSystemPromptAssembler.AssembleContext, String>) context ->
                        "## Runtime context\n- Working directory: "
                                + (context.cwd() == null || context.cwd().isBlank()
                                        ? System.getProperty("user.dir") : context.cwd())
        ));
        return assembler;
    }

    @Bean
    public SubagentRegistry subagentRegistry() {
        SubagentRegistry registry = new SubagentRegistry();
        // Provider 会在构造完成后通过 agentFactorySubagentInit() 注册
        return registry;
    }

    @Bean
    public Object agentFactorySubagentInit(
            SubagentRegistry subagentRegistry,
            AgentFactory agentFactory
    ) {
        // 注册进程内 Spawn Provider，供模型通过子智能体工具委托任务
        var spawn = new cn.walioffice.deepseek.harness.domain.agent.subagent.SpawnInProcessProvider(
                "spawn-in-process",
                subagentRegistry,
                agentFactory,
                new cn.walioffice.deepseek.harness.domain.agent.model.entity.AgentOptions(
                        "deepseek", "gpt-5.5", 4096, null, null)
        );
        subagentRegistry.register(spawn);
        return spawn;
    }

    @Bean
    public CompactionEngine compactionEngine(
            LlmGenerationService llmGenerationService
    ) {
        return new BasicCompactionEngine(
                llmGenerationService,
                100_000,  // pressure threshold ~100k tokens
                6         // min messages before compaction
        );
    }

    @Bean
    public LlmGenerationService llmGenerationService(ILlmRuntimePort llmRuntimePort) {
        return new LlmGenerationService(llmRuntimePort);
    }

    @Bean
    public InMemoryUserQuestionsPort userQuestionsPort() {
        return new InMemoryUserQuestionsPort();
    }

    /**
     * 运行期审批 Broker：agent 执行到需审批工具时实时挂起，
     * 由 REST 端点提交裁决后唤醒。对应原版 TS 的 gate.kind==='ask' 语义。
     */
    @Bean
    public RuntimeApprovalBroker runtimeApprovalBroker(
            @Value("${harness.approval.runtime-timeout-ms:600000}") long timeoutMs
    ) {
        return new RuntimeApprovalBroker(timeoutMs);
    }

    /**
     * 创建计划模式领域服务。
     * <p>
     * 执行流程：
     * 1. 直接实例化纯领域服务；
     * 2. 由工具层在执行时调用；
     * 3. 保持基础设施层只负责装配，不承载业务逻辑。
     */
    @Bean
    public PlanModeService planModeService() {
        return new PlanModeService();
    }

    /**
     * 创建待办写入领域服务。
     * <p>
     * 执行流程：
     * 1. 直接实例化纯领域服务；
     * 2. 由 `todo_write` 工具在执行时调用；
     * 3. 将校验与事件写入收敛到领域层。
     */
    @Bean
    public TodoWriteService todoWriteService() {
        return new TodoWriteService();
    }

    // ── 第二阶段：Shell + 文件系统基础设施 Bean ────────────────────

    @Bean
    public IShellExecutor shellExecutor(
            ISandboxPolicyService sandboxPolicyService,
            @Value("${harness.agent.default-cwd:#{systemProperties['user.dir']}}") String defaultCwd,
            @Value("${harness.sandbox.shell-enabled:true}") boolean sandboxEnabled
    ) {
        IShellExecutor raw = new LocalShellExecutor();
        // 沙箱开关：默认启用，可通过 harness.sandbox.shell-enabled=false 回退裸执行
        return sandboxEnabled
                ? new SandboxedShellExecutor(raw, sandboxPolicyService, defaultCwd)
                : raw;
    }

    @Bean
    public IFsPort fsService(
            @Value("${harness.agent.default-cwd:#{systemProperties['user.dir']}}") String defaultCwd,
            ISandboxPolicyService sandboxPolicyService,
            @Value("${harness.sandbox.fs-enabled:true}") boolean sandboxEnabled
    ) {
        return sandboxEnabled
                ? new SandboxedFsService(defaultCwd, sandboxPolicyService)
                : new LocalFsService(defaultCwd);
    }

    @Bean
    public IWebPort webService(
            @Value("${harness.web.search-api-base:}") String searchApiBase,
            @Value("${harness.web.search-api-key:}") String searchApiKey
    ) {
        if (searchApiBase != null && !searchApiBase.isBlank() && searchApiKey != null && !searchApiKey.isBlank()) {
            return new LocalWebService(searchApiBase, searchApiKey);
        }
        return new LocalWebService();
    }

    @Bean
    public AgentFactory agentFactory(
            ILlmRuntimePort llmRuntimePort,
            ToolRegistry toolRegistry,
            SystemPromptAssembler systemPromptAssembler,
            cn.walioffice.deepseek.harness.domain.agent.ask.UserQuestionsPort userQuestionsPort,
            IShellExecutor shellExecutor,
            IFsPort fsService,
            IWebPort webService,
            PlanModeService planModeService,
            TodoWriteService todoWriteService,
            SubagentRegistry subagentRegistry,
            IHookService hookService,
            ObjectMapper objectMapper
    ) {
        return new AgentFactory(
                llmRuntimePort,
                toolRegistry,
                systemPromptAssembler,
                userQuestionsPort,
                shellExecutor,
                fsService,
                webService,
                planModeService,
                todoWriteService,
                subagentRegistry,
                hookService,
                objectMapper
        );
    }

    // ── 凭据层 ────────────────────────────────────────

    @Bean
    public ICredentialProviderPort credentialProviderPort(
            @Value("${harness.credentials.home:${user.home}/.dsh}") String dshHome
    ) {
        LocalCredentialProviderPort port = new LocalCredentialProviderPort(Path.of(dshHome));
        port.init();
        return port;
    }

    @Bean
    public ICredentialService credentialService(ICredentialProviderPort credentialProviderPort) {
        return new CredentialService(credentialProviderPort);
    }

    // ── 存储层 ────────────────────────────────────────────

    @Bean
    public IStorageBackendPort storageBackendPort(
            ObjectMapper objectMapper,
            @Value("${harness.storage.root:#{systemProperties['user.home']}/.dsh/storage}") String storageRoot
    ) {
        return new JsonStorageBackendPort(Path.of(storageRoot), objectMapper);
    }

    @Bean
    public IStorageService storageService(IStorageBackendPort storageBackendPort) {
        return new StorageService(storageBackendPort);
    }

    // ── 守卫层 ───────────────────────────────────────────────

    @Bean
    public IGuardService guardService() {
        return new GuardService();
    }

    @Bean
    public GoalService goalService(
            cn.walioffice.deepseek.harness.domain.goal.adapter.repository.IGoalRepository goalRepository,
            @Value("${harness.goal.default-max-rounds:256}") int defaultMaxRounds
    ) {
        return new GoalService(goalRepository, defaultMaxRounds);
    }

    @Bean
    public ITerminalPort terminalPort(
            @Value("${harness.terminal.default-cwd:#{systemProperties['user.dir']}}") String defaultCwd
    ) {
        return new LocalTerminalPort(defaultCwd);
    }

    @Bean
    public TerminalService terminalService(ITerminalPort terminalPort) {
        return new TerminalService(terminalPort);
    }

    // ── 沙箱层 ────────────────────────────────────────────

    @Bean
    public ISandboxPolicyService sandboxPolicyService(
            @Value("${harness.sandbox.default-mode:READ_ONLY}") String defaultMode,
            @Value("${harness.agent.default-cwd:#{systemProperties['user.dir']}}") String fallbackRoot
    ) {
        SandboxMode mode = SandboxMode.valueOf(defaultMode.trim().toUpperCase().replace('-', '_'));
        return new SandboxPolicyService(mode, Path.of(fallbackRoot));
    }

    // ── 调度层 ───────────────────────────────────────────

    @Bean
    public IScheduleRepository scheduleRepository() {
        return new InMemoryScheduleRepository();
    }

    @Bean
    public IScheduleService scheduleService(IScheduleRepository scheduleRepository) {
        return new ScheduleService(scheduleRepository);
    }

    // ── 任务层 ───────────────────────────────────────────────

    @Bean
    public IJobRegistryRepository jobRegistryRepository() {
        return new InMemoryJobRegistryRepository();
    }

    @Bean
    public IJobRegistryService jobRegistryService(IJobRegistryRepository jobRegistryRepository) {
        return new JobRegistryService(jobRegistryRepository);
    }

    // ── 钩子层 ──────────────────────────────────────────────

    @Bean
    public IHookRunnerPort hookRunnerPort() {
        return new LocalShellHookRunner();
    }

    @Bean
    public IHookService hookService(IHookRunnerPort hookRunnerPort) {
        return new HookService(hookRunnerPort);
    }

    // ── 技能层 ──────────────────────────────────────────────

    @Bean
    public ISkillProviderPort skillProviderPort(
            @Value("${harness.credentials.home:${user.home}/.dsh}") String dshHome
    ) {
        return new FilesystemSkillProviderPort(Path.of(dshHome), null);
    }

    @Bean
    public ISkillService skillService(List<ISkillProviderPort> skillProviders) {
        return new SkillService(skillProviders);
    }

    // ── LSP 层 ────────────────────────────────────────────────

    @Bean
    public ILspService lspService() {
        return new LspService();
    }

    // ── 代码运行时层 ───────────────────────────────────────

    @Bean
    public ICodeRuntimePort codeRuntimePort() {
        return new LocalCodeRuntimePort();
    }

    @Bean
    public ICodeRuntimeService codeRuntimeService(ICodeRuntimePort codeRuntimePort) {
        return new CodeRuntimeService(codeRuntimePort);
    }

    // ── E2B 层 ────────────────────────────────────────────────

    @Bean
    public IE2BSandboxService e2bSandboxService(
            @Value("${harness.e2b.api-key:}") String apiKey,
            @Value("${harness.e2b.template:base}") String template
    ) {
        // 本地模式下 E2B 文件系统和子进程端口仅提供占位实现；
        // 真正接入时会调用 E2B SDK。
        IE2BFsPort fsPortImpl = new StubE2BFsPort();
        
        IE2BSubprocessPort subprocessPort = new StubE2BSubprocessPort(); //
        return new E2BSandboxService(fsPortImpl, subprocessPort);
    }

    // ── ACP 层 ────────────────────────────────────────────────

    @Bean
    public IAcpAgentPort acpAgentPort(
            @Value("${harness.acp.command:}") String command
    ) {
        if (command == null || command.isBlank()) {
            return new StubAcpAgentPort();
        }
        return new StdioAcpAgentPort(command, List.of());
    }

    @Bean
    public IAcpServerService acpServerService(IAcpAgentPort acpAgentPort) {
        return new AcpServerService(acpAgentPort);
    }

    // ── 子智能体进程外提供方 ────────────────────────

    @Bean
    public AcpSubagentProvider acpSubagentProvider(
            @Value("${harness.subagent.acp.command:}") String command,
            @Value("${harness.agent.default-cwd:#{systemProperties['user.dir']}}") String cwd
    ) {
        if (command == null || command.isBlank()) return null;
        return new AcpSubagentProvider(command, List.of(), cwd, Map.of(), 300_000);
    }

    @Bean
    public CodexSubagentProvider codexSubagentProvider(
            @Value("${harness.subagent.codex.command:codex}") String command,
            @Value("${harness.agent.default-cwd:#{systemProperties['user.dir']}}") String cwd
    ) {
        return new CodexSubagentProvider(command, List.of(), cwd, Map.of(), 300_000);
    }

    @Bean
    public ClaudeCodeSubagentProvider claudeCodeSubagentProvider(
            @Value("${harness.subagent.claude-code.command:claude}") String command,
            @Value("${harness.agent.default-cwd:#{systemProperties['user.dir']}}") String cwd
    ) {
        return new ClaudeCodeSubagentProvider(command, List.of(), cwd, Map.of(), 300_000);
    }

    // ── 工作流层 ───────────────────────────────────────────

    @Bean
    public IWorkflowEnginePort workflowEnginePort() {
        return new LocalWorkflowEnginePort();
    }

    @Bean
    public IWorkflowService workflowService(IWorkflowEnginePort workflowEnginePort) {
        return new WorkflowService(workflowEnginePort);
    }

    // ── Typert 层 ─────────────────────────────────────────────

    @Bean
    public ITypertRegistryContract typertRegistryContract() {
        return new InMemoryTypertRegistry();
    }

    @Bean
    public ITypertService typertService(ITypertRegistryContract typertRegistryContract) {
        return new TypertService(typertRegistryContract);
    }

    @Bean
    public TypertProtocolLoader typertProtocolLoader(ObjectMapper objectMapper) {
        return new TypertProtocolLoader(objectMapper);
    }

    // ── SDK 层 ────────────────────────────────────────────────

    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "harness.sdk.standalone", havingValue = "true")
    public SdkServerService sdkServerService(
            StdioSdkTransportPort stdioSdkTransportPort,
            cn.walioffice.deepseek.harness.domain.sdk.adapter.port.ISdkServerHandler sdkServerHandler
    ) {
        return new SdkServerService(stdioSdkTransportPort, sdkServerHandler);
    }

    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "harness.sdk.standalone", havingValue = "true")
    public StdioSdkTransportPort stdioSdkTransportPort(ObjectMapper objectMapper) {
        return new StdioSdkTransportPort(System.in, System.out, objectMapper);
    }

    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "harness.sdk.standalone", havingValue = "true")
    public ISdkClientService sdkClientService(StdioSdkTransportPort stdioSdkTransportPort) {
        return new SdkClientService(stdioSdkTransportPort);
    }

    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "harness.sdk.standalone", havingValue = "true")
    public cn.walioffice.deepseek.harness.domain.sdk.adapter.port.ISdkServerHandler sdkServerHandler() {
        return (method, params) -> switch (method) {
            case "initialize" -> java.util.Map.of(
                    "serverName", "deepseek-harness-sdk-runtime",
                    "serverVersion", "1.0.0"
            );
            case "shutdown" -> java.util.Map.of();
            default -> throw new IllegalArgumentException("unknown method: " + method);
        };
    }
}
