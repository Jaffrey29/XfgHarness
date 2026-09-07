package cn.xiaofuge.deepseek.harness.infrastructure.config;

import cn.xiaofuge.deepseek.harness.domain.agent.ask.UserQuestionsPort;
import cn.xiaofuge.deepseek.harness.domain.agent.model.entity.AgentOptions;
import cn.xiaofuge.deepseek.harness.domain.agent.subagent.SpawnInProcessProvider;
import cn.xiaofuge.deepseek.harness.domain.llm.model.valobj.LlmErrorCodes;
import cn.xiaofuge.deepseek.harness.domain.session.event.adapter.repository.ISessionEventStore;
import cn.xiaofuge.deepseek.harness.domain.plan.service.PlanModeService;
import cn.xiaofuge.deepseek.harness.domain.todo.service.TodoWriteService;
import cn.xiaofuge.deepseek.harness.domain.terminal.adapter.port.ITerminalPort;
import cn.xiaofuge.deepseek.harness.domain.sandbox.ISandboxPolicyService;
import cn.xiaofuge.deepseek.harness.domain.llm.adapter.LlmAdapter;
import cn.xiaofuge.deepseek.harness.domain.llm.adapter.ResolvedRetryPolicy;
import cn.xiaofuge.deepseek.harness.domain.llm.adapter.port.ILlmRuntimePort;
import cn.xiaofuge.deepseek.harness.domain.agent.service.SystemPromptAssembler;
import cn.xiaofuge.deepseek.harness.domain.agent.service.ScopedSystemPromptAssembler;
import cn.xiaofuge.deepseek.harness.domain.agent.subagent.SubagentRegistry;
import cn.xiaofuge.deepseek.harness.domain.tool.adapter.ToolRegistry;
import cn.xiaofuge.deepseek.harness.domain.tool.fs.adapter.port.IFsPort;
import cn.xiaofuge.deepseek.harness.domain.tool.shell.IShellExecutor;
import cn.xiaofuge.deepseek.harness.domain.tool.web.adapter.port.IWebPort;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.agent.AgentFactory;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.agent.InMemoryUserQuestionsPort;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.agent.RuntimeApprovalBroker;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.fs.LocalFsService;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.fs.SandboxedFsService;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.llm.InMemoryLlmRuntimePort;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.llm.openai.OpenAiCompatibleAdapter;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.llm.deepseek.DeepSeekAdapter;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.llm.deepseek.DeepSeekAdapterOptions;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.llm.deepseek.DirectSeekCatalogModel;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.sandbox.SandboxedShellExecutor;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.shell.LocalShellExecutor;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.tool.InMemoryToolRegistry;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.web.LocalWebService;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.xiaofuge.deepseek.harness.domain.typert.adapter.port.ITypertRegistryContract;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.typert.InMemoryTypertRegistry;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.typert.TypertProtocolLoader;

import cn.xiaofuge.deepseek.harness.infrastructure.adapter.acp.StubAcpAgentPort;

import cn.xiaofuge.deepseek.harness.domain.workflow.adapter.port.IWorkflowEnginePort;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.workflow.LocalWorkflowEnginePort;

import cn.xiaofuge.deepseek.harness.domain.acp.adapter.port.IAcpAgentPort;
import cn.xiaofuge.deepseek.harness.domain.coderuntime.adapter.port.ICodeRuntimePort;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.acp.StdioAcpAgentPort;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.coderuntime.LocalCodeRuntimePort;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.subagent.AcpSubagentProvider;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.subagent.ClaudeCodeSubagentProvider;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.subagent.CodexSubagentProvider;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.terminal.LocalTerminalPort;

import cn.xiaofuge.deepseek.harness.domain.skill.adapter.port.ISkillProviderPort;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.skill.FilesystemSkillProviderPort;

import cn.xiaofuge.deepseek.harness.domain.hooks.IHookService;
import cn.xiaofuge.deepseek.harness.domain.hooks.adapter.port.IHookRunnerPort;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.hooks.LocalShellHookRunner;

import cn.xiaofuge.deepseek.harness.domain.jobs.adapter.repository.IJobRegistryRepository;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.jobs.InMemoryJobRegistryRepository;

import cn.xiaofuge.deepseek.harness.domain.schedule.adapter.repository.IScheduleRepository;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.schedule.InMemoryScheduleRepository;


import cn.xiaofuge.deepseek.harness.domain.storage.adapter.port.IStorageBackendPort;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.storage.JsonStorageBackendPort;
import cn.xiaofuge.deepseek.harness.domain.tool.plugin.IPluginToolBridge;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.plugin.JsonRpcPluginToolBridge;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.tool.JacksonToolArgumentsParser;
import cn.xiaofuge.deepseek.harness.domain.tool.service.ToolArgumentsParser;
import java.nio.file.Path;

import cn.xiaofuge.deepseek.harness.domain.credentials.adapter.port.ICredentialProviderPort;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.credentials.LocalCredentialProviderPort;

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
 * <p>调用流程：领域端口 → 基础设施适配器 → 外部系统。</p>
 * <p>示例：适配器只实现端口契约，业务规则保留在 domain。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Configuration
public class HarnessApplicationConfig {

    @Bean
    public IPluginToolBridge jsonRpcPluginToolBridge() {
        return new JsonRpcPluginToolBridge();
    }

    @Bean
    public ToolArgumentsParser toolArgumentsParser(ObjectMapper objectMapper) {
        return new JacksonToolArgumentsParser(objectMapper);
    }

    // ── 大模型层 Bean ───────────────────────────────────────
    /**
     * llm Runtime Port。
     * 流程：适配外部协议或资源 → 调用底层能力 → 返回结果。
     * @param adapterList adapter List参数。
     * @return 方法执行结果。
     */


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
                LlmErrorCodes.DEFAULT_RETRYABLE_CODES
        );
        var options = new DeepSeekAdapterOptions(baseUrl, apiKey, maxTokens, contextWindow,
                DeepSeekAdapter.DEFAULT_STREAM_IDLE_TIMEOUT_MS, models, retryPolicy);
        return new DeepSeekAdapter(options, objectMapper);
    }
    /**
     * openai Adapter。
     * 流程：适配外部协议或资源 → 调用底层能力 → 返回结果。
     * @param objectMapper object Mapper参数。
     * @return 方法执行结果。
     */


    @Bean
    public OpenAiCompatibleAdapter openaiAdapter(ObjectMapper objectMapper) {
        var options = new DeepSeekAdapterOptions(
                "https://api.openai.com/v1",
                System.getenv("OPENAI_API_KEY") != null ? System.getenv("OPENAI_API_KEY") : "",
                4096, 128000, DeepSeekAdapter.DEFAULT_STREAM_IDLE_TIMEOUT_MS,
                List.of(), null);
        return new OpenAiCompatibleAdapter("openai", options, objectMapper);
    }
    /**
     * ollama Adapter。
     * 流程：适配外部协议或资源 → 调用底层能力 → 返回结果。
     * @param objectMapper object Mapper参数。
     * @return 方法执行结果。
     */


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
    /**
     * 转换为ol Registry。
     * 流程：适配外部协议或资源 → 调用底层能力 → 返回结果。
     * @return 转换后的目标对象。
     */


    @Bean
    public ToolRegistry toolRegistry() {
        return new InMemoryToolRegistry();
    }
    /**
     * system Prompt Assembler。
     * 流程：适配外部协议或资源 → 调用底层能力 → 返回结果。
     * @return 方法执行结果。
     */


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
    /**
     * agent Factory Subagent Init。
     * 流程：适配外部协议或资源 → 调用底层能力 → 返回结果。
     * @param subagentRegistry subagent Registry参数。
     * @param agentFactory agent Factory参数。
     * @return 方法执行结果。
     */


    @Bean
    public Object agentFactorySubagentInit(
            SubagentRegistry subagentRegistry,
            AgentFactory agentFactory
    ) {
        // 注册进程内 Spawn Provider，供模型通过子智能体工具委托任务
        var spawn = new SpawnInProcessProvider(
                "spawn-in-process",
                subagentRegistry,
                agentFactory,
                new AgentOptions(
                        "deepseek", "gpt-5.5", 4096, null, null)
        );
        subagentRegistry.register(spawn);
        return spawn;
    }
    /**
     * user Questions Port。
     * 流程：适配外部协议或资源 → 调用底层能力 → 返回结果。
     * @return 方法执行结果。
     */


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
    /**
     * agent Factory。
     * 流程：适配外部协议或资源 → 调用底层能力 → 返回结果。
     * @param llmRuntimePort llm Runtime Port参数。
     * @param toolRegistry tool Registry参数。
     * @param systemPromptAssembler system Prompt Assembler参数。
     * @param userQuestionsPort user Questions Port参数。
     * @param shellExecutor shell Executor参数。
     * @param fsService fs Service参数。
     * @param webService web Service参数。
     * @param planModeService plan Mode Service参数。
     * @param todoWriteService todo Write Service参数。
     * @param subagentRegistry subagent Registry参数。
     * @param hookService hook Service参数。
     * @param objectMapper object Mapper参数。
     * @param sessionEventStore session Event Store参数。
     * @return 方法执行结果。
     */


    @Bean
    public AgentFactory agentFactory(
            ILlmRuntimePort llmRuntimePort,
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
            ISessionEventStore sessionEventStore
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
                objectMapper,
                null,
                null,
                sessionEventStore
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

    // ── 存储层 ────────────────────────────────────────────

    @Bean
    public IStorageBackendPort storageBackendPort(
            ObjectMapper objectMapper,
            @Value("${harness.storage.root:#{systemProperties['user.home']}/.dsh/storage}") String storageRoot
    ) {
        return new JsonStorageBackendPort(Path.of(storageRoot), objectMapper);
    }

    // ── 守卫层 ───────────────────────────────────────────────

    @Bean
    public ITerminalPort terminalPort(
            @Value("${harness.terminal.default-cwd:#{systemProperties['user.dir']}}") String defaultCwd
    ) {
        return new LocalTerminalPort(defaultCwd);
    }

    // ── 调度层 ───────────────────────────────────────────
    /**
     * schedule Repository。
     * 流程：适配外部协议或资源 → 调用底层能力 → 返回结果。
     * @return 方法执行结果。
     */


    @Bean
    public IScheduleRepository scheduleRepository() {
        return new InMemoryScheduleRepository();
    }

    // ── 任务层 ───────────────────────────────────────────────
    /**
     * job Registry Repository。
     * 流程：适配外部协议或资源 → 调用底层能力 → 返回结果。
     * @return 方法执行结果。
     */


    @Bean
    public IJobRegistryRepository jobRegistryRepository() {
        return new InMemoryJobRegistryRepository();
    }

    // ── 钩子层 ──────────────────────────────────────────────
    /**
     * hook Runner Port。
     * 流程：适配外部协议或资源 → 调用底层能力 → 返回结果。
     * @return 方法执行结果。
     */


    @Bean
    public IHookRunnerPort hookRunnerPort() {
        return new LocalShellHookRunner();
    }

    // ── 技能层 ──────────────────────────────────────────────

    @Bean
    public ISkillProviderPort skillProviderPort(
            @Value("${harness.credentials.home:${user.home}/.dsh}") String dshHome
    ) {
        return new FilesystemSkillProviderPort(Path.of(dshHome), null);
    }

    // ── LSP 层 ────────────────────────────────────────────────

    // ── 代码运行时层 ───────────────────────────────────────
    /**
     * code Runtime Port。
     * 流程：适配外部协议或资源 → 调用底层能力 → 返回结果。
     * @return 方法执行结果。
     */


    @Bean
    public ICodeRuntimePort codeRuntimePort() {
        return new LocalCodeRuntimePort();
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
    /**
     * workflow Engine Port。
     * 流程：适配外部协议或资源 → 调用底层能力 → 返回结果。
     * @return 方法执行结果。
     */


    @Bean
    public IWorkflowEnginePort workflowEnginePort() {
        return new LocalWorkflowEnginePort();
    }

    // ── Typert 层 ─────────────────────────────────────────────
    /**
     * typert Registry Contract。
     * 流程：适配外部协议或资源 → 调用底层能力 → 返回结果。
     * @return 方法执行结果。
     */


    @Bean
    public ITypertRegistryContract typertRegistryContract() {
        return new InMemoryTypertRegistry();
    }
    /**
     * typert Protocol Loader。
     * 流程：适配外部协议或资源 → 调用底层能力 → 返回结果。
     * @param objectMapper object Mapper参数。
     * @return 方法执行结果。
     */


    @Bean
    public TypertProtocolLoader typertProtocolLoader(ObjectMapper objectMapper) {
        return new TypertProtocolLoader(objectMapper);
    }

}
