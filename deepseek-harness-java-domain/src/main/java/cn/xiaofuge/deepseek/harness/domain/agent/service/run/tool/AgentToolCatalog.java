package cn.xiaofuge.deepseek.harness.domain.agent.service.run.tool;

import cn.xiaofuge.deepseek.harness.domain.agent.contract.outbound.UserQuestionGateway;
import cn.xiaofuge.deepseek.harness.domain.agent.service.ask.AskUserTool;
import cn.xiaofuge.deepseek.harness.domain.agent.service.subagent.SubagentRegistry;
import cn.xiaofuge.deepseek.harness.domain.agent.service.subagent.SubagentTool;
import cn.xiaofuge.deepseek.harness.domain.plan.ExitPlanModeTool;
import cn.xiaofuge.deepseek.harness.domain.plan.model.valobj.PlanModeState;
import cn.xiaofuge.deepseek.harness.domain.plan.service.PlanModeService;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionLog;
import cn.xiaofuge.deepseek.harness.domain.todo.service.TodoWriteService;
import cn.xiaofuge.deepseek.harness.domain.todo.service.TodoWriteTool;
import cn.xiaofuge.deepseek.harness.domain.tool.adapter.ToolRegistry;
import cn.xiaofuge.deepseek.harness.domain.tool.fs.FsReadTool;
import cn.xiaofuge.deepseek.harness.domain.tool.fs.FsSearchTool;
import cn.xiaofuge.deepseek.harness.domain.tool.fs.FsWriteTool;
import cn.xiaofuge.deepseek.harness.domain.tool.fs.StrReplaceEditorTool;
import cn.xiaofuge.deepseek.harness.domain.tool.fs.adapter.port.IFsPort;
import cn.xiaofuge.deepseek.harness.domain.tool.shell.IShellExecutor;
import cn.xiaofuge.deepseek.harness.domain.tool.shell.ShellExecuteTool;
import cn.xiaofuge.deepseek.harness.domain.tool.web.WebFetchTool;
import cn.xiaofuge.deepseek.harness.domain.tool.web.WebSearchTool;
import cn.xiaofuge.deepseek.harness.domain.tool.web.adapter.port.IWebPort;

import java.util.concurrent.atomic.AtomicReference;

public final class AgentToolCatalog {

    private final IShellExecutor shellExecutor;
    private final IFsPort fsPort;
    private final IWebPort webPort;
    private final PlanModeService planModeService;
    private final TodoWriteService todoWriteService;
    private final SubagentRegistry subagentRegistry;

    public AgentToolCatalog(
            IShellExecutor shellExecutor,
            IFsPort fsPort,
            IWebPort webPort,
            PlanModeService planModeService,
            TodoWriteService todoWriteService,
            SubagentRegistry subagentRegistry
    ) {
        this.shellExecutor = shellExecutor;
        this.fsPort = fsPort;
        this.webPort = webPort;
        this.planModeService = planModeService;
        this.todoWriteService = todoWriteService;
        this.subagentRegistry = subagentRegistry;
    }

    public ToolRegistry createRegistry(AgentToolContext context, ToolRegistry sharedRegistry) {
        InMemoryToolRegistry localRegistry = new InMemoryToolRegistry();
        AtomicReference<PlanModeState> planState =
                new AtomicReference<>(new PlanModeState.Off());

        localRegistry.register(new TodoWriteTool(context.session(), false, todoWriteService));
        localRegistry.register(new AskUserTool(context.userQuestions(), context.agentId()));
        localRegistry.register(new ExitPlanModeTool(context.session(), planState, planModeService));
        localRegistry.register(new ShellExecuteTool(shellExecutor, context.cwd()));
        localRegistry.register(new FsReadTool(fsPort));
        localRegistry.register(new FsWriteTool(fsPort));
        localRegistry.register(new FsSearchTool(fsPort));
        localRegistry.register(new StrReplaceEditorTool(fsPort));
        localRegistry.register(new WebSearchTool(webPort));
        localRegistry.register(new WebFetchTool(webPort));

        if (subagentRegistry != null && !subagentRegistry.listProviderNames().isEmpty()) {
            String providerName = subagentRegistry.listProviderNames().get(0);
            localRegistry.register(new SubagentTool(subagentRegistry, providerName));
        }

        return new CompositeToolRegistry(localRegistry, sharedRegistry);
    }

    public record AgentToolContext(
            String agentId,
            String cwd,
            SessionLog session,
            UserQuestionGateway userQuestions
    ) {}
}
