package cn.xiaofuge.deepseek.harness.infrastructure.config;

import cn.xiaofuge.deepseek.harness.domain.agent.contract.outbound.UserQuestionGateway;
import cn.xiaofuge.deepseek.harness.domain.agent.contract.inbound.AgentRunLifecycle;
import cn.xiaofuge.deepseek.harness.domain.agent.contract.outbound.RuntimeApprovalGateway;
import cn.xiaofuge.deepseek.harness.domain.agent.service.run.AgentRunFactory;
import cn.xiaofuge.deepseek.harness.domain.agent.service.run.InMemoryUserQuestionGateway;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.agent.PersistingAgentEventListener;
import cn.xiaofuge.deepseek.harness.domain.agent.service.run.PersistingSessionLog;
import cn.xiaofuge.deepseek.harness.domain.agent.service.run.RuntimeApprovalBroker;
import cn.xiaofuge.deepseek.harness.domain.agent.service.run.tool.InMemoryToolRegistry;
import cn.xiaofuge.deepseek.harness.domain.agent.service.run.tool.AgentToolCatalog;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.tool.JacksonToolArgumentsParser;
import cn.xiaofuge.deepseek.harness.domain.agent.service.SystemPromptAssembler;
import cn.xiaofuge.deepseek.harness.domain.agent.service.ScopedSystemPromptAssembler;
import cn.xiaofuge.deepseek.harness.domain.agent.service.subagent.SubagentRegistry;
import cn.xiaofuge.deepseek.harness.domain.agent.service.subagent.SpawnInProcessProvider;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.service.AgentModelSettingResolver;
import cn.xiaofuge.deepseek.harness.domain.goal.IGoalCommandService;
import cn.xiaofuge.deepseek.harness.domain.goal.IGoalQueryService;
import cn.xiaofuge.deepseek.harness.domain.jobs.IJobRegistryService;
import cn.xiaofuge.deepseek.harness.domain.plugin.IPluginRuntimeService;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.adapter.repository.IHarnessPluginRepository;
import cn.xiaofuge.deepseek.harness.domain.lsp.ILspService;
import cn.xiaofuge.deepseek.harness.domain.schedule.IScheduleService;
import cn.xiaofuge.deepseek.harness.domain.session.IConversationQueryService;
import cn.xiaofuge.deepseek.harness.domain.hooks.IHookService;
import cn.xiaofuge.deepseek.harness.domain.llm.adapter.port.ILlmRuntimePort;
import cn.xiaofuge.deepseek.harness.domain.llm.service.LlmGenerationService;
import cn.xiaofuge.deepseek.harness.domain.agent.service.compaction.BasicCompactionEngine;
import cn.xiaofuge.deepseek.harness.domain.agent.service.compaction.CompactionEngine;
import cn.xiaofuge.deepseek.harness.domain.plan.service.PlanModeService;
import cn.xiaofuge.deepseek.harness.domain.session.event.adapter.repository.ISessionEventStore;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionHeader;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionLog;
import cn.xiaofuge.deepseek.harness.domain.task.permission.adapter.port.IPermissionMatrixPort;
import cn.xiaofuge.deepseek.harness.domain.todo.service.TodoWriteService;
import cn.xiaofuge.deepseek.harness.domain.tool.adapter.ToolRegistry;
import cn.xiaofuge.deepseek.harness.domain.tool.service.ToolArgumentsParser;
import cn.xiaofuge.deepseek.harness.domain.tool.shell.IShellExecutor;
import cn.xiaofuge.deepseek.harness.domain.tool.fs.adapter.port.IFsPort;
import cn.xiaofuge.deepseek.harness.domain.tool.web.adapter.port.IWebPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import java.util.function.Function;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AgentRun 运行时组合根。
 * <p>负责把领域核心、外部能力端口和运行期治理策略组装成可执行的 Agent。</p>
 */
@Configuration
public class AgentRunBeanConfig {

    @Bean
    public LlmGenerationService llmGenerationService(ILlmRuntimePort llmRuntimePort) {
        return new LlmGenerationService(llmRuntimePort);
    }

    @Bean
    public CompactionEngine compactionEngine(
            LlmGenerationService llmGenerationService,
            @Value("${harness.agent.compaction.pressure-threshold-tokens:100000}") long pressureThresholdTokens,
            @Value("${harness.agent.compaction.min-messages-to-compact:6}") int minMessagesToCompact
    ) {
        return new BasicCompactionEngine(llmGenerationService, pressureThresholdTokens, minMessagesToCompact);
    }

    @Bean
    public SubagentRegistry subagentRegistry() {
        return new SubagentRegistry();
    }

    @Bean
    public ToolRegistry toolRegistry() {
        return new InMemoryToolRegistry();
    }

    @Bean
    public ToolArgumentsParser toolArgumentsParser(ObjectMapper objectMapper) {
        return new JacksonToolArgumentsParser(objectMapper);
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
                - 不要在正文中输出内部推理过程或 <think> 之类的标签，只输出最终回答。
                """;
        var assembler = new ScopedSystemPromptAssembler(identity, persona, List.of());
        assembler.registerContext(new ScopedSystemPromptAssembler.PromptContext(
                "harness:runtime",
                20,
                (Function<ScopedSystemPromptAssembler.AssembleContext, String>) context -> "## Runtime context\n- Working directory: "
                        + (context.cwd() == null || context.cwd().isBlank()
                                ? System.getProperty("user.dir") : context.cwd())
        ));
        return assembler;
    }

    @Bean
    public InMemoryUserQuestionGateway userQuestionsGateway() {
        return new InMemoryUserQuestionGateway();
    }

    @Bean
    public RuntimeApprovalBroker runtimeApprovalBroker(
            @Value("${harness.approval.runtime-timeout-ms:600000}") long timeoutMs
    ) {
        return new RuntimeApprovalBroker(timeoutMs);
    }

    @Bean
    public Object agentRunFactorySubagentInit(
            SubagentRegistry subagentRegistry,
            AgentRunLifecycle agentRunFactory,
            AgentModelSettingResolver modelSettingResolver
    ) {
        var spawn = new SpawnInProcessProvider(
                "spawn-in-process",
                subagentRegistry,
                agentRunFactory,
                () -> modelSettingResolver.resolve(null, 4096)
        );
        subagentRegistry.register(spawn);
        return spawn;
    }

    @Bean
    public AgentRunFactory agentRunFactory(
            ILlmRuntimePort llmRuntimePort,
            ToolRegistry toolRegistry,
            SystemPromptAssembler systemPromptAssembler,
            UserQuestionGateway userQuestions,
            IHookService hookService,
            ToolArgumentsParser toolArgumentsParser,
            IPermissionMatrixPort permissionMatrixPort,
            RuntimeApprovalGateway runtimeApprovalBroker,
            SubagentRegistry subagentRegistry,
            IShellExecutor shellExecutor,
            IFsPort fsService,
            IWebPort webService,
            IJobRegistryService jobRegistryService,
            IGoalQueryService goalQueryService,
            IGoalCommandService goalCommandService,
            IScheduleService scheduleService,
            ILspService lspService,
            IConversationQueryService conversationQueryService,
            cn.xiaofuge.deepseek.harness.domain.terminal.adapter.port.ITerminalPort terminalPort,
            PlanModeService planModeService,
            TodoWriteService todoWriteService,
            cn.xiaofuge.deepseek.harness.domain.skill.ISkillService skillService,
            ObjectMapper objectMapper,
            ObjectProvider<ISessionEventStore> sessionEventStoreProvider,
            CompactionEngine compactionEngine,
            IHarnessPluginRepository pluginRepository,
            IPluginRuntimeService pluginRuntimeService
    ) {
        ISessionEventStore sessionEventStore = sessionEventStoreProvider.getIfAvailable();
        AgentRunFactory.SessionLogFactory sessionLogFactory = header -> sessionEventStore == null
                ? new SessionLog(header.sessionId(), header)
                : new PersistingSessionLog(header.sessionId(), header, sessionEventStore);
        AgentRunFactory.AgentEventListenerFactory agentEventListenerFactory = sessionEventStore == null
                ? null
                : sessionId -> new PersistingAgentEventListener(sessionId, sessionEventStore, objectMapper);

        AgentToolCatalog toolCatalog = new AgentToolCatalog(
                shellExecutor,
                fsService,
                webService,
                planModeService,
                todoWriteService,
                subagentRegistry,
                skillService,
                jobRegistryService,
                goalQueryService,
                goalCommandService,
                scheduleService,
                lspService,
                conversationQueryService,
                terminalPort,
                pluginRepository,
                pluginRuntimeService
        );

        return new AgentRunFactory(
                llmRuntimePort,
                toolRegistry,
                systemPromptAssembler,
                userQuestions,
                toolCatalog,
                toolArgumentsParser,
                hookService,
                permissionMatrixPort,
                runtimeApprovalBroker,
                sessionLogFactory,
                agentEventListenerFactory,
                compactionEngine
        );
    }
}
