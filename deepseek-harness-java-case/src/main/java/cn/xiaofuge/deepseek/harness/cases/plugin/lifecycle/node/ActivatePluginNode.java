package cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle.node;

import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle.PluginRuntimeCommand;
import cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle.PluginRuntimeContext;
import cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle.PluginRuntimeOutcome;
import cn.xiaofuge.deepseek.harness.api.dto.ActivatePluginResponseDTO;
import org.springframework.stereotype.Service;

@Service
public class ActivatePluginNode
        extends AbstractStrategyRouter<PluginRuntimeCommand, PluginRuntimeContext, PluginRuntimeOutcome> {

    @Override
    protected PluginRuntimeOutcome doApply(
            PluginRuntimeCommand request,
            PluginRuntimeContext context
    ) {
        var plan = context.getBridgePlan();
        if (!plan.bridgeReady()) {
            throw new IllegalStateException("plugin bridge is not ready: " + plan.message());
        }
        return new PluginRuntimeOutcome.Activation(new ActivatePluginResponseDTO(
                context.getPlugin().getPluginId(),
                context.getStatus().name(),
                plan.packageType().name(),
                plan.runtimeCommand(),
                plan.bridgeReady(),
                plan.message(),
                context.getCheckedAt()
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
