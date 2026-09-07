package cn.xiaofuge.deepseek.harness.cases.plugin.manage.factory;

import cn.xiaofuge.deepseek.harness.cases.orchestration.CasePipeline;
import cn.xiaofuge.deepseek.harness.cases.orchestration.CasePipelineFactory;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.api.dto.PluginLifecycleResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.plugin.manage.node.ManagePluginNode;
import org.springframework.stereotype.Service;

@Service
public class PluginManagePipelineFactory
        implements CasePipelineFactory<ManagePluginNode.Input, NoContext, PluginLifecycleResponseDTO> {

    private final ManagePluginNode managePluginNode;

    public PluginManagePipelineFactory(ManagePluginNode managePluginNode) {
        this.managePluginNode = managePluginNode;
    }

    @Override
    public CasePipeline<ManagePluginNode.Input, NoContext, PluginLifecycleResponseDTO> pipeline() {
        return CasePipeline.of("Manage plugin", managePluginNode, () -> NoContext.INSTANCE);
    }
}
