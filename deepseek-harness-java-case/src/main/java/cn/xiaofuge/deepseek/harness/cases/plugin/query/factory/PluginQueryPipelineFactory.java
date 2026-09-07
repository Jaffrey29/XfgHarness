package cn.xiaofuge.deepseek.harness.cases.plugin.query.factory;

import cn.xiaofuge.deepseek.harness.cases.orchestration.CasePipeline;
import cn.xiaofuge.deepseek.harness.cases.orchestration.CasePipelineFactory;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.api.dto.QueryPluginStatusResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.InstalledPluginResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.plugin.query.node.ListInstalledPluginsNode;
import cn.xiaofuge.deepseek.harness.cases.plugin.query.node.QueryPluginStatusNode;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 插件查询 Pipeline 工厂。
 * <p>流程：按查询目标组装 Pipeline；{@code statusPipeline} 查询运行状态，{@code installedPipeline} 查询安装清单。</p>
 * <p>数据示例：状态查询传入 {@code pluginId=weather-forecast}，返回 {@code running=true, processId=8102}；
 * 安装查询无入参，返回插件 DTO 列表。</p>
 */
@Service
public class PluginQueryPipelineFactory
        implements CasePipelineFactory<String, NoContext, QueryPluginStatusResponseDTO> {

    private final QueryPluginStatusNode queryPluginStatusNode;
    private final ListInstalledPluginsNode listInstalledPluginsNode;

    public PluginQueryPipelineFactory(
            QueryPluginStatusNode queryPluginStatusNode,
            ListInstalledPluginsNode listInstalledPluginsNode
    ) {
        this.queryPluginStatusNode = queryPluginStatusNode;
        this.listInstalledPluginsNode = listInstalledPluginsNode;
    }

    /**
     * 构建插件运行状态查询 Pipeline。
     */
    public CasePipeline<String, NoContext, QueryPluginStatusResponseDTO> statusPipeline() {
        return CasePipeline.of("Query plugin status", queryPluginStatusNode, () -> NoContext.INSTANCE);
    }

    /**
     * 构建已安装插件列表查询 Pipeline。
     */
    public CasePipeline<Void, NoContext, List<InstalledPluginResponseDTO>> installedPipeline() {
        return CasePipeline.of("List installed plugins", listInstalledPluginsNode, () -> NoContext.INSTANCE);
    }

    /**
     * 构建默认执行 Pipeline，绑定首节点和上下文。
     */
    @Override
    public CasePipeline<String, NoContext, QueryPluginStatusResponseDTO> pipeline() {
        return statusPipeline();
    }
}
