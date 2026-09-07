package cn.xiaofuge.deepseek.harness.cases.plugin.run;

import cn.xiaofuge.deepseek.harness.cases.plugin.QueryPluginStatusResult;
import cn.xiaofuge.deepseek.harness.cases.plugin.IQueryHarnessPluginStatusCase;
import cn.xiaofuge.deepseek.harness.domain.plugin.runtime.model.valobj.PluginProcessStatusVO;
import cn.xiaofuge.deepseek.harness.domain.plugin.IPluginRuntimeService;
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
    public QueryPluginStatusResult execute(String pluginId) {
        PluginProcessStatusVO result = pluginRuntimeService.status(pluginId);
        return new QueryPluginStatusResult(
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
