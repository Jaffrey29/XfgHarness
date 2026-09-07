package cn.xiaofuge.deepseek.harness.cases.plugin.query.node;

import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.api.dto.InstalledPluginResponseDTO;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.adapter.repository.IHarnessPluginRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ListInstalledPluginsNode
        extends AbstractStrategyRouter<Void, NoContext, List<InstalledPluginResponseDTO>> {

    private final IHarnessPluginRepository pluginRepository;

    public ListInstalledPluginsNode(IHarnessPluginRepository pluginRepository) {
        this.pluginRepository = pluginRepository;
    }

    @Override
    protected List<InstalledPluginResponseDTO> doApply(Void request, NoContext context) {
        return pluginRepository.listAll().stream().map(plugin -> new InstalledPluginResponseDTO(
                plugin.getPluginId(), plugin.getDisplayName(), plugin.getPluginVersion(),
                plugin.getRuntimeType().name(), plugin.getInstallMode().name(), plugin.getStatus().name(),
                plugin.getSourcePath(), plugin.getEntrypoint(), plugin.getInstalledAt()
        )).toList();
    }

    @Override
    public StrategyHandler<Void, NoContext, List<InstalledPluginResponseDTO>> getNext(Void request, NoContext context) {
        return defaultStrategyHandler;
    }
}
