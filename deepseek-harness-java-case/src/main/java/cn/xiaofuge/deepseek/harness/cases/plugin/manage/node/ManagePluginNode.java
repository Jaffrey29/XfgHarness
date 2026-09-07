package cn.xiaofuge.deepseek.harness.cases.plugin.manage.node;

import cn.xiaofuge.deepseek.harness.api.dto.PluginLifecycleResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.cases.support.CaseGuard;
import cn.xiaofuge.deepseek.harness.domain.plugin.IPluginRegistryService;
import cn.xiaofuge.deepseek.harness.domain.plugin.IPluginRuntimeService;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class ManagePluginNode
        extends AbstractStrategyRouter<ManagePluginNode.Input, NoContext, PluginLifecycleResponseDTO> {

    private final IPluginRegistryService pluginRegistryService;
    private final IPluginRuntimeService pluginRuntimeService;

    public ManagePluginNode(
            IPluginRegistryService pluginRegistryService,
            IPluginRuntimeService pluginRuntimeService
    ) {
        this.pluginRegistryService = pluginRegistryService;
        this.pluginRuntimeService = pluginRuntimeService;
    }

    @Override
    protected PluginLifecycleResponseDTO doApply(Input input, NoContext context) {
        CaseGuard.requireText(input.pluginId(), "pluginId must not be blank");
        return switch (input.action()) {
            case DISABLE -> disable(input.pluginId());
            case ENABLE -> enable(input.pluginId());
            case UNINSTALL -> uninstall(input.pluginId());
            case STOP -> stop(input.pluginId());
        };
    }

    @Override
    public StrategyHandler<Input, NoContext, PluginLifecycleResponseDTO> getNext(Input input, NoContext context) {
        return defaultStrategyHandler;
    }

    private PluginLifecycleResponseDTO disable(String pluginId) {
        pluginRuntimeService.stop(pluginId);
        var status = pluginRegistryService.disablePlugin(pluginId);
        return new PluginLifecycleResponseDTO(pluginId, status.name(), "plugin disabled", Instant.now());
    }

    private PluginLifecycleResponseDTO enable(String pluginId) {
        var status = pluginRegistryService.enablePlugin(pluginId);
        return new PluginLifecycleResponseDTO(pluginId, status.name(), "plugin enabled", Instant.now());
    }

    private PluginLifecycleResponseDTO uninstall(String pluginId) {
        pluginRuntimeService.stop(pluginId);
        var status = pluginRegistryService.uninstallPlugin(pluginId);
        return new PluginLifecycleResponseDTO(pluginId, status.name(), "plugin uninstalled", Instant.now());
    }

    private PluginLifecycleResponseDTO stop(String pluginId) {
        var result = pluginRuntimeService.stop(pluginId);
        return new PluginLifecycleResponseDTO(
                pluginId,
                result.running() ? "RUNNING" : "STOPPED",
                result.message(),
                Instant.now()
        );
    }

    public enum Action {
        DISABLE,
        ENABLE,
        UNINSTALL,
        STOP
    }

    public record Input(Action action, String pluginId) {
    }
}
