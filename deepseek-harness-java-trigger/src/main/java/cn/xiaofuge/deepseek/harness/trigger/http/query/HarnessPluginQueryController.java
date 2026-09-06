package cn.xiaofuge.deepseek.harness.trigger.http.query;

import cn.xiaofuge.deepseek.harness.api.dto.plugin.QueryPluginStatusResponse;
import cn.xiaofuge.deepseek.harness.api.response.plugin.InstalledPluginResponse;
import cn.xiaofuge.deepseek.harness.trigger.service.HarnessPluginQueryApi;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 插件查询控制器。
 * <p>
 * 执行流程：
 * 1. 接收前端插件查询请求；
 * 2. 调用插件查询适配器读取安装态或运行态信息；
 * 3. 直接返回 JSON 响应体。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@RestController
@RequestMapping("/api/harness/plugins")
public class HarnessPluginQueryController {

    private final HarnessPluginQueryApi harnessPluginQueryApi;

    public HarnessPluginQueryController(HarnessPluginQueryApi harnessPluginQueryApi) {
        this.harnessPluginQueryApi = harnessPluginQueryApi;
    }

    /**
     * 查询已安装插件列表。
     * <p>
     * 执行流程：
     * 1. 调用查询适配器读取插件列表；
     * 2. 将结果保持原样返回；
     * 3. 供前端插件中心直接渲染。
     */
    @GetMapping
    public List<InstalledPluginResponse> listInstalledPlugins() {
        return harnessPluginQueryApi.listInstalledPlugins();
    }

    /**
     * 查询单个插件运行状态。
     * <p>
     * 执行流程：
     * 1. 从路径参数中读取插件标识；
     * 2. 调用查询适配器获取运行状态；
     * 3. 返回插件状态响应。
     */
    @GetMapping("/{pluginId}/status")
    public QueryPluginStatusResponse queryPluginStatus(@PathVariable("pluginId") String pluginId) {
        return harnessPluginQueryApi.queryPluginStatus(pluginId);
    }
}
