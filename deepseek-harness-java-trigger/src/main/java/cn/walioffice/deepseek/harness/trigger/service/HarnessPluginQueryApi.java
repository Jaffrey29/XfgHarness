package cn.walioffice.deepseek.harness.trigger.service;

import cn.walioffice.deepseek.harness.api.dto.plugin.QueryPluginStatusResponse;
import cn.walioffice.deepseek.harness.api.response.plugin.InstalledPluginResponse;
import cn.walioffice.deepseek.harness.api.service.query.HarnessInstalledPluginQueryApi;
import cn.walioffice.deepseek.harness.cases.plugin.run.IQueryHarnessPluginStatusCase;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 插件查询适配器。
 * <p>
 * 执行流程：
 * 1. 接收 Trigger 层的插件查询请求；
 * 2. 调用领域查询服务或查询用例组装读模型；
 * 3. 返回前端可直接渲染的响应对象。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class HarnessPluginQueryApi {

    private final HarnessInstalledPluginQueryApi harnessInstalledPluginQueryApi;
    private final IQueryHarnessPluginStatusCase queryHarnessPluginStatusCase;

    public HarnessPluginQueryApi(HarnessInstalledPluginQueryApi harnessInstalledPluginQueryApi,
                                 IQueryHarnessPluginStatusCase queryHarnessPluginStatusCase) {
        this.harnessInstalledPluginQueryApi = harnessInstalledPluginQueryApi;
        this.queryHarnessPluginStatusCase = queryHarnessPluginStatusCase;
    }

    public List<InstalledPluginResponse> listInstalledPlugins() {
        return harnessInstalledPluginQueryApi.listInstalledPlugins();
    }

    public QueryPluginStatusResponse queryPluginStatus(String pluginId) {
        return queryHarnessPluginStatusCase.execute(pluginId);
    }
}
