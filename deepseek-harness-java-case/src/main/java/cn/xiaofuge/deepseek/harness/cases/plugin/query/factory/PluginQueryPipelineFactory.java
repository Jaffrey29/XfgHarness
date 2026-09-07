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

    public CasePipeline<String, NoContext, QueryPluginStatusResponseDTO> statusPipeline() {
        return CasePipeline.of("Query plugin status", queryPluginStatusNode, () -> NoContext.INSTANCE);
    }

    public CasePipeline<Void, NoContext, List<InstalledPluginResponseDTO>> installedPipeline() {
        return CasePipeline.of("List installed plugins", listInstalledPluginsNode, () -> NoContext.INSTANCE);
    }

    @Override
    public CasePipeline<String, NoContext, QueryPluginStatusResponseDTO> pipeline() {
        return statusPipeline();
    }
}
