package cn.xiaofuge.deepseek.harness.cases.plugin.install.node;

import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.api.dto.InstallPluginRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.InstallPluginResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.support.CaseGuard;
import cn.xiaofuge.deepseek.harness.domain.plugin.IPluginRegistryService;
import org.springframework.stereotype.Service;

@Service
public class InstallPluginNode
        extends AbstractStrategyRouter<InstallPluginRequestDTO, NoContext, InstallPluginResponseDTO> {

    private final IPluginRegistryService pluginRegistryService;

    public InstallPluginNode(IPluginRegistryService pluginRegistryService) {
        this.pluginRegistryService = pluginRegistryService;
    }

    @Override
    protected InstallPluginResponseDTO doApply(InstallPluginRequestDTO request, NoContext context) {
        CaseGuard.requireNonNull(request, "install command must not be null");
        CaseGuard.requireText(request.pluginId(), "pluginId must not be blank");
        var result = pluginRegistryService.installPlugin(
                request.pluginId(),
                request.displayName(),
                request.pluginVersion(),
                request.runtimeType(),
                request.sourcePath(),
                request.entrypoint()
        );
        return new InstallPluginResponseDTO(
                result.pluginId(),
                result.runtimeType().name(),
                result.installMode().name(),
                result.status().name(),
                result.directlyExecutable(),
                result.installedAt()
        );
    }

    @Override
    public StrategyHandler<InstallPluginRequestDTO, NoContext, InstallPluginResponseDTO> getNext(
            InstallPluginRequestDTO request, NoContext context) {
        return defaultStrategyHandler;
    }
}
