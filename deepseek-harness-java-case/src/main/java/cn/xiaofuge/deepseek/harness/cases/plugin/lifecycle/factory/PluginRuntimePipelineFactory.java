package cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle.factory;

import cn.xiaofuge.deepseek.harness.cases.orchestration.CasePipeline;
import cn.xiaofuge.deepseek.harness.cases.orchestration.CasePipelineFactory;
import cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle.PluginRuntimeCommand;
import cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle.PluginRuntimeContext;
import cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle.PluginRuntimeOutcome;
import cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle.node.ActivatePluginNode;
import cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle.node.BindBridgeNode;
import cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle.node.InspectBridgeNode;
import cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle.node.ResolvePluginNode;
import cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle.node.RunPluginNode;
import org.springframework.stereotype.Service;

/**
 * 插件运行期 Pipeline 工厂。
 * <p>流程：创建上下文 → ResolvePluginNode → InspectBridgeNode → BindBridgeNode → 按 kind 路由到 ActivatePluginNode 或 RunPluginNode。</p>
 * <p>数据示例：传入 {@code pluginId=weather-forecast, kind=ACTIVATE}；
 * 返回 {@code Activation(result.bridgeReady=true, packageType=PYTHON)}。</p>
 */
@Service
public class PluginRuntimePipelineFactory
        implements CasePipelineFactory<PluginRuntimeCommand, PluginRuntimeContext, PluginRuntimeOutcome> {

    private final ResolvePluginNode resolvePluginNode;
    private final InspectBridgeNode inspectBridgeNode;
    private final BindBridgeNode bindBridgeNode;
    private final ActivatePluginNode activatePluginNode;
    private final RunPluginNode runPluginNode;

    public PluginRuntimePipelineFactory(
            ResolvePluginNode resolvePluginNode,
            InspectBridgeNode inspectBridgeNode,
            BindBridgeNode bindBridgeNode,
            ActivatePluginNode activatePluginNode,
            RunPluginNode runPluginNode
    ) {
        this.resolvePluginNode = resolvePluginNode;
        this.inspectBridgeNode = inspectBridgeNode;
        this.bindBridgeNode = bindBridgeNode;
        this.activatePluginNode = activatePluginNode;
        this.runPluginNode = runPluginNode;
    }

    /**
     * 构建默认执行 Pipeline，绑定首节点和上下文。
     */
    public CasePipeline<PluginRuntimeCommand, PluginRuntimeContext, PluginRuntimeOutcome> pipeline() {
        return CasePipeline.of(
                "Plugin runtime lifecycle",
                resolvePluginNode,
                PluginRuntimeContext::new
        );
    }

    /**
     * 构建当前用例的执行 Pipeline，绑定首节点和上下文。
     */
    public CasePipeline<PluginRuntimeCommand, PluginRuntimeContext, PluginRuntimeOutcome> pluginPipeline() {
        return pipeline();
    }
}
