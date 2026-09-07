package cn.xiaofuge.deepseek.harness.cases.plugin.run;

import cn.xiaofuge.deepseek.harness.cases.plugin.QueryPluginStatusResult;
import cn.xiaofuge.deepseek.harness.cases.plugin.IQueryHarnessPluginStatusCase;
import cn.xiaofuge.deepseek.harness.domain.plugin.runtime.model.valobj.PluginProcessStatusVO;
import cn.xiaofuge.deepseek.harness.domain.plugin.IPluginRuntimeService;
import org.springframework.stereotype.Service;

/**
 * 协调插件进程状态查询。
 *
 * <p>调用流程：Controller/API → 插件用例 → 领域插件服务 → PluginRuntime/Bridge。</p>
 * <p>示例：安装插件后先激活，再通过统一工具名调用插件工具。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class QueryHarnessPluginStatusCaseImpl implements IQueryHarnessPluginStatusCase {

    private final IPluginRuntimeService pluginRuntimeService;

    /**
     * 查询Harness Plugin Status Case Impl。
     */
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
