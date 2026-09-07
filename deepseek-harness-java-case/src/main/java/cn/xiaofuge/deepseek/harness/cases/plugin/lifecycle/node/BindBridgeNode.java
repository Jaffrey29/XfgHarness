package cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle.node;

import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle.PluginRuntimeCommand;
import cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle.PluginRuntimeContext;
import cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle.PluginRuntimeOutcome;
import cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle.PluginRuntimeKind;
import cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle.node.ActivatePluginNode;
import cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle.node.RunPluginNode;
import cn.xiaofuge.deepseek.harness.domain.plugin.IPluginBridgeService;
import cn.xiaofuge.deepseek.harness.domain.plugin.IPluginRegistryService;
import org.springframework.stereotype.Service;

@Service
public class BindBridgeNode
        extends AbstractStrategyRouter<PluginRuntimeCommand, PluginRuntimeContext, PluginRuntimeOutcome> {

    private final IPluginRegistryService pluginRegistryService;
    private final IPluginBridgeService pluginBridgeService;
    private final ActivatePluginNode activatePluginNode;
    private final RunPluginNode runPluginNode;

    public BindBridgeNode(
            IPluginRegistryService pluginRegistryService,
            IPluginBridgeService pluginBridgeService,
            ActivatePluginNode activatePluginNode,
            RunPluginNode runPluginNode
    ) {
        this.pluginRegistryService = pluginRegistryService;
        this.pluginBridgeService = pluginBridgeService;
        this.activatePluginNode = activatePluginNode;
        this.runPluginNode = runPluginNode;
    }

    @Override
    protected PluginRuntimeOutcome doApply(
            PluginRuntimeCommand request,
            PluginRuntimeContext context
    ) {
        context.setStatus(pluginRegistryService.refreshStatus(
                context.getPlugin(), context.getBridgePlan().bridgeReady()
        ));
        pluginBridgeService.recordBinding(
                context.getPlugin().getPluginId(),
                context.getBridgePlan(),
                context.getCheckedAt()
        );
        return null;
    }

    @Override
    public StrategyHandler<PluginRuntimeCommand, PluginRuntimeContext, PluginRuntimeOutcome> getNext(
            PluginRuntimeCommand request,
            PluginRuntimeContext context
    ) {
        return request.kind() == PluginRuntimeKind.ACTIVATE ? activatePluginNode : runPluginNode;
    }
}
