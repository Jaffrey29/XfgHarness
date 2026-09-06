package cn.walioffice.deepseek.harness.cases.plugin.run;

import cn.walioffice.deepseek.harness.api.dto.plugin.QueryPluginStatusResponse;
import cn.walioffice.deepseek.harness.domain.plugin.runtime.model.valobj.PluginProcessStatusVO;
import cn.walioffice.deepseek.harness.domain.plugin.IPluginRuntimeService;
import org.springframework.stereotype.Service;

/**
 * 协调插件进程状态查询。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class QueryHarnessPluginStatusCaseImpl implements IQueryHarnessPluginStatusCase {

    private final IPluginRuntimeService pluginRuntimeService;

    public QueryHarnessPluginStatusCaseImpl(IPluginRuntimeService pluginRuntimeService) {
        this.pluginRuntimeService = pluginRuntimeService;
    }

    @Override
    public QueryPluginStatusResponse execute(String pluginId) {
        PluginProcessStatusVO result = pluginRuntimeService.status(pluginId);
        return new QueryPluginStatusResponse(
                result.pluginId(),
                result.processId(),
                result.running(),
                result.runtimeCommand(),
                result.workingDirectory(),
                result.startedAt(),
                result.message()
        );
    }
}
