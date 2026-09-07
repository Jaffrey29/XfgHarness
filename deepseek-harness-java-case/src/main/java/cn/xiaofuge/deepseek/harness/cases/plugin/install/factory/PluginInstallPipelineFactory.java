package cn.xiaofuge.deepseek.harness.cases.plugin.install.factory;

import cn.xiaofuge.deepseek.harness.cases.orchestration.CasePipeline;
import cn.xiaofuge.deepseek.harness.cases.orchestration.CasePipelineFactory;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.api.dto.InstallPluginRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.InstallPluginResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.plugin.install.node.InstallPluginNode;
import org.springframework.stereotype.Service;

/**
 * 插件安装 Pipeline 工厂。
 * <p>流程：组装无共享上下文的安装 Pipeline → 执行 InstallPluginNode → 返回安装结果。</p>
 * <p>数据示例：传入 {@code pluginId=weather-forecast, sourcePath=/plugins/weather}；
 * 返回 {@code status=INSTALLED, directlyExecutable=true}。</p>
 */
@Service
public class PluginInstallPipelineFactory
        implements CasePipelineFactory<InstallPluginRequestDTO, NoContext, InstallPluginResponseDTO> {

    private final InstallPluginNode installPluginNode;

    public PluginInstallPipelineFactory(InstallPluginNode installPluginNode) {
        this.installPluginNode = installPluginNode;
    }

    /**
     * 构建默认执行 Pipeline，绑定首节点和上下文。
     */
    @Override
    public CasePipeline<InstallPluginRequestDTO, NoContext, InstallPluginResponseDTO> pipeline() {
        return CasePipeline.of("Install plugin", installPluginNode, () -> NoContext.INSTANCE);
    }
}
