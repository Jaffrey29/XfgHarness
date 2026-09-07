package cn.xiaofuge.deepseek.harness.cases.plugin;

import cn.xiaofuge.deepseek.harness.api.dto.InstalledPluginResponseDTO;

import cn.xiaofuge.deepseek.harness.cases.plugin.query.factory.PluginQueryPipelineFactory;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class InstalledPluginQueryCaseImpl implements IInstalledPluginQueryCase {
    private final PluginQueryPipelineFactory pluginQueryPipelineFactory;

    public InstalledPluginQueryCaseImpl(PluginQueryPipelineFactory pluginQueryPipelineFactory) {
        this.pluginQueryPipelineFactory = pluginQueryPipelineFactory;
    }

    @Override
    public List<InstalledPluginResponseDTO> listInstalledPlugins() {
        return pluginQueryPipelineFactory.installedPipeline().execute(null);
    }
}
