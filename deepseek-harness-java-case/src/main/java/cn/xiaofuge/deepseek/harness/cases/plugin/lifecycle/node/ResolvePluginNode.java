package cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle.node;

import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle.PluginRuntimeCommand;
import cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle.PluginRuntimeContext;
import cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle.PluginRuntimeOutcome;
import cn.xiaofuge.deepseek.harness.cases.support.CaseGuard;
import cn.xiaofuge.deepseek.harness.domain.plugin.IPluginRegistryService;
import org.springframework.stereotype.Service;

@Service
public class ResolvePluginNode
        extends AbstractStrategyRouter<PluginRuntimeCommand, PluginRuntimeContext, PluginRuntimeOutcome> {

    private final IPluginRegistryService pluginRegistryService;
    private final InspectBridgeNode inspectBridgeNode;

    public ResolvePluginNode(
            IPluginRegistryService pluginRegistryService,
            InspectBridgeNode inspectBridgeNode
    ) {
        this.pluginRegistryService = pluginRegistryService;
        this.inspectBridgeNode = inspectBridgeNode;
    }

    @Override
    protected PluginRuntimeOutcome doApply(
            PluginRuntimeCommand request,
            PluginRuntimeContext context
    ) {
        CaseGuard.requireText(request.pluginId(), "pluginId must not be blank");
        context.setPlugin(pluginRegistryService.loadPlugin(request.pluginId()));
        return null;
    }

    @Override
    public StrategyHandler<PluginRuntimeCommand, PluginRuntimeContext, PluginRuntimeOutcome> getNext(
            PluginRuntimeCommand request,
            PluginRuntimeContext context
    ) {
        return inspectBridgeNode;
    }
}
