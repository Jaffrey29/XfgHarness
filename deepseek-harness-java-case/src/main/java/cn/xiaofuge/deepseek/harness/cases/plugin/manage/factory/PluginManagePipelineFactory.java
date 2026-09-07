package cn.xiaofuge.deepseek.harness.cases.plugin.manage.factory;

import cn.xiaofuge.deepseek.harness.cases.orchestration.CasePipeline;
import cn.xiaofuge.deepseek.harness.cases.orchestration.CasePipelineFactory;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.api.dto.PluginLifecycleResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.plugin.manage.node.ManagePluginNode;
import org.springframework.stereotype.Service;

/**
 * 插件管理 Pipeline 工厂。
 * <p>流程：组装管理 Pipeline → 执行 ManagePluginNode → 返回生命周期结果。</p>
 * <p>数据示例：传入 {@code action=DISABLE, pluginId=weather-forecast}；
 * 返回 {@code status=DISABLED, message="plugin disabled"}。</p>
 */
@Service
public class PluginManagePipelineFactory
        implements CasePipelineFactory<ManagePluginNode.Input, NoContext, PluginLifecycleResponseDTO> {

    private final ManagePluginNode managePluginNode;

    public PluginManagePipelineFactory(ManagePluginNode managePluginNode) {
        this.managePluginNode = managePluginNode;
    }

    /**
     * 构建默认执行 Pipeline，绑定首节点和上下文。
     */
    @Override
    public CasePipeline<ManagePluginNode.Input, NoContext, PluginLifecycleResponseDTO> pipeline() {
        return CasePipeline.of("Manage plugin", managePluginNode, () -> NoContext.INSTANCE);
    }
}
