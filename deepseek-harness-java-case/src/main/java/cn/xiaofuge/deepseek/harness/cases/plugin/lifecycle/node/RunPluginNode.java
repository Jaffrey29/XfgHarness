package cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle.node;

import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle.PluginRuntimeCommand;
import cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle.PluginRuntimeContext;
import cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle.PluginRuntimeOutcome;
import cn.xiaofuge.deepseek.harness.domain.plugin.IPluginRuntimeService;
import org.springframework.stereotype.Service;

@Service
public class RunPluginNode
        extends AbstractStrategyRouter<PluginRuntimeCommand, PluginRuntimeContext, PluginRuntimeOutcome> {

    private final IPluginRuntimeService pluginRuntimeService;

    public RunPluginNode(IPluginRuntimeService pluginRuntimeService) {
        this.pluginRuntimeService = pluginRuntimeService;
    }

    @Override
    protected PluginRuntimeOutcome doApply(
            PluginRuntimeCommand request,
            PluginRuntimeContext context
    ) {
        var plan = context.getBridgePlan();
        if (!plan.bridgeReady()) {
            throw new IllegalStateException("plugin bridge is not ready: " + plan.message());
        }
        var result = pluginRuntimeService.start(context.getPlugin(), plan);
return new PluginRuntimeOutcome.Run(new cn.xiaofuge.deepseek.harness.api.dto.RunPluginResponseDTO(
                result.pluginId(),
                result.processId(),
                result.runtimeCommand(),
                result.workingDirectory(),
                result.running(),
                result.startedAt(),
                result.message()
        ));
    }

    @Override
    public StrategyHandler<PluginRuntimeCommand, PluginRuntimeContext, PluginRuntimeOutcome> getNext(
            PluginRuntimeCommand request,
            PluginRuntimeContext context
    ) {
        return defaultStrategyHandler;
    }
}
