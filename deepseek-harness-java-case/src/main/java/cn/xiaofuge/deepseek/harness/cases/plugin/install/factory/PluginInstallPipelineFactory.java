package cn.xiaofuge.deepseek.harness.cases.plugin.install.factory;

import cn.xiaofuge.deepseek.harness.cases.orchestration.CasePipeline;
import cn.xiaofuge.deepseek.harness.cases.orchestration.CasePipelineFactory;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.api.dto.InstallPluginRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.InstallPluginResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.plugin.install.node.InstallPluginNode;
import org.springframework.stereotype.Service;

@Service
public class PluginInstallPipelineFactory
        implements CasePipelineFactory<InstallPluginRequestDTO, NoContext, InstallPluginResponseDTO> {

    private final InstallPluginNode installPluginNode;

    public PluginInstallPipelineFactory(InstallPluginNode installPluginNode) {
        this.installPluginNode = installPluginNode;
    }

    @Override
    public CasePipeline<InstallPluginRequestDTO, NoContext, InstallPluginResponseDTO> pipeline() {
        return CasePipeline.of("Install plugin", installPluginNode, () -> NoContext.INSTANCE);
    }
}
