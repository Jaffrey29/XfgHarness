package cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle.node;

import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle.PluginRuntimeCommand;
import cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle.PluginRuntimeContext;
import cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle.PluginRuntimeOutcome;
import cn.xiaofuge.deepseek.harness.domain.plugin.IPluginBridgeService;
import org.springframework.stereotype.Service;

@Service
public class InspectBridgeNode
        extends AbstractStrategyRouter<PluginRuntimeCommand, PluginRuntimeContext, PluginRuntimeOutcome> {

    private final IPluginBridgeService pluginBridgeService;
    private final BindBridgeNode bindBridgeNode;

    public InspectBridgeNode(
            IPluginBridgeService pluginBridgeService,
            BindBridgeNode bindBridgeNode
    ) {
        this.pluginBridgeService = pluginBridgeService;
        this.bindBridgeNode = bindBridgeNode;
    }

    @Override
    protected PluginRuntimeOutcome doApply(
            PluginRuntimeCommand request,
            PluginRuntimeContext context
    ) {
        context.setCheckedAt(java.time.Instant.now());
        context.setBridgePlan(pluginBridgeService.inspect(context.getPlugin()));
        return null;
    }

    @Override
    public StrategyHandler<PluginRuntimeCommand, PluginRuntimeContext, PluginRuntimeOutcome> getNext(
            PluginRuntimeCommand request,
            PluginRuntimeContext context
    ) {
        return bindBridgeNode;
    }
}
