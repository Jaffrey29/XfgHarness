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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 插件桥接绑定节点。
 * <p>流程：按桥接检查结果刷新插件状态 → 记录绑定 → 根据 kind 路由到激活或运行节点。</p>
 * <p>数据示例：桥接就绪时保存 {@code status=ENABLED}；{@code kind=RUN} 转到运行节点，{@code kind=ACTIVATE} 转到激活节点。</p>
 */
@Service
public class BindBridgeNode
        extends AbstractStrategyRouter<PluginRuntimeCommand, PluginRuntimeContext, PluginRuntimeOutcome> {

    private static final Logger log = LoggerFactory.getLogger(BindBridgeNode.class);

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

    /**
     * 执行当前节点核心逻辑：校验输入、调用领域服务并返回输出。
     */
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
        log.info("[Plugin] 插件桥接绑定完成，pluginId={} status={} kind={}",
                context.getPlugin().getPluginId(), context.getStatus(), request.kind());
        return null;
    }

    /**
     * 返回下一执行节点；单节点链路返回默认终止处理。
     */
    @Override
    public StrategyHandler<PluginRuntimeCommand, PluginRuntimeContext, PluginRuntimeOutcome> getNext(
            PluginRuntimeCommand request,
            PluginRuntimeContext context
    ) {
        return request.kind() == PluginRuntimeKind.ACTIVATE ? activatePluginNode : runPluginNode;
    }
}
