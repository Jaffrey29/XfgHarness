package cn.xiaofuge.deepseek.harness.trigger.service;

import cn.xiaofuge.deepseek.harness.api.dto.plugin.QueryPluginStatusResponse;
import cn.xiaofuge.deepseek.harness.api.response.plugin.InstalledPluginResponse;
import cn.xiaofuge.deepseek.harness.api.service.query.HarnessInstalledPluginQueryApi;
import cn.xiaofuge.deepseek.harness.cases.plugin.IQueryHarnessPluginStatusCase;
import cn.xiaofuge.deepseek.harness.cases.plugin.QueryPluginStatusResult;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 插件查询适配器。
 * <p>
 * 执行流程：
 * 1. 接收 Trigger 层的插件查询请求；
 * 2. 调用领域查询服务或查询用例组装读模型；
 * 3. 将 case 层 Result 映射为 api Response 返回前端可直接渲染的响应对象。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class HarnessPluginQueryApi {

    private final HarnessInstalledPluginQueryApi harnessInstalledPluginQueryApi;
    private final IQueryHarnessPluginStatusCase queryHarnessPluginStatusCase;

    /**
     * 处理Harness Plugin Query Api相关逻辑。
     */
    public HarnessPluginQueryApi(HarnessInstalledPluginQueryApi harnessInstalledPluginQueryApi,
                                 IQueryHarnessPluginStatusCase queryHarnessPluginStatusCase) {
        this.harnessInstalledPluginQueryApi = harnessInstalledPluginQueryApi;
        this.queryHarnessPluginStatusCase = queryHarnessPluginStatusCase;
    }

    /**
     * 列出Installed Plugins。
     */
    public List<InstalledPluginResponse> listInstalledPlugins() {
        return harnessInstalledPluginQueryApi.listInstalledPlugins();
    }

    /**
     * 查询Plugin Status。
     */
    public QueryPluginStatusResponse queryPluginStatus(String pluginId) {
        QueryPluginStatusResult r = queryHarnessPluginStatusCase.execute(pluginId);
        return new QueryPluginStatusResponse(
                r.pluginId(), r.processId(), r.running(),
                r.runtimeCommand(), r.workingDirectory(), r.startedAt(), r.message());
    }
}
