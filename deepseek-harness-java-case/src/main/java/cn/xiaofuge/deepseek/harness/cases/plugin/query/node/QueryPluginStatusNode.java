package cn.xiaofuge.deepseek.harness.cases.plugin.query.node;

import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.api.dto.QueryPluginStatusResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.support.CaseGuard;
import cn.xiaofuge.deepseek.harness.domain.plugin.IPluginRuntimeService;
import org.springframework.stereotype.Service;

@Service
public class QueryPluginStatusNode extends AbstractStrategyRouter<String, NoContext, QueryPluginStatusResponseDTO> {

    private final IPluginRuntimeService pluginRuntimeService;

    public QueryPluginStatusNode(IPluginRuntimeService pluginRuntimeService) {
        this.pluginRuntimeService = pluginRuntimeService;
    }

    @Override
    protected QueryPluginStatusResponseDTO doApply(String pluginId, NoContext context) {
        CaseGuard.requireText(pluginId, "pluginId must not be blank");
        var result = pluginRuntimeService.status(pluginId);
        return new QueryPluginStatusResponseDTO(
                result.pluginId(),
                result.processId(),
                result.running(),
                result.runtimeCommand(),
                result.workingDirectory(),
                result.startedAt(),
                result.message()
        );
    }

    @Override
    public StrategyHandler<String, NoContext, QueryPluginStatusResponseDTO> getNext(String pluginId, NoContext context) {
        return defaultStrategyHandler;
    }
}
