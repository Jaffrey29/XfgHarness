package cn.xiaofuge.deepseek.harness.trigger.http.command;

import cn.xiaofuge.deepseek.harness.api.dto.plugin.ActivatePluginRequest;
import cn.xiaofuge.deepseek.harness.api.dto.plugin.ActivatePluginResponse;
import cn.xiaofuge.deepseek.harness.api.dto.plugin.InstallPluginRequest;
import cn.xiaofuge.deepseek.harness.api.dto.plugin.InstallPluginResponse;
import cn.xiaofuge.deepseek.harness.api.dto.plugin.PluginLifecycleResponse;
import cn.xiaofuge.deepseek.harness.api.dto.plugin.RunPluginRequest;
import cn.xiaofuge.deepseek.harness.api.dto.plugin.RunPluginResponse;
import cn.xiaofuge.deepseek.harness.trigger.service.HarnessPluginCommandApi;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 面向 Web 控制台暴露插件写操作。 */
@RestController
@RequestMapping("/api/harness/plugins")
public class HarnessPluginCommandController {

    private final HarnessPluginCommandApi harnessPluginCommandApi;

    /**
     * 处理Harness 插件 命令 Controller。
     * 流程：解析请求参数 → 调用应用门面 → 返回响应。
     * @param harnessPluginCommandApi harness 插件 命令 API参数。
     * @return 方法执行结果。
     */
    public HarnessPluginCommandController(HarnessPluginCommandApi harnessPluginCommandApi) {
        this.harnessPluginCommandApi = harnessPluginCommandApi;
    }

    /**
     * 安装 插件。
     * 流程：解析请求参数 → 调用应用门面 → 返回响应。
     * @param request 请求对象。
     * @return 方法执行结果。
     */
    @PostMapping("/install")
    public InstallPluginResponse installPlugin(@RequestBody InstallPluginRequest request) {
        return harnessPluginCommandApi.installPlugin(request);
    }

    /**
     * 激活 插件。
     * 流程：解析请求参数 → 调用应用门面 → 返回响应。
     * @param request 请求对象。
     * @return 方法执行结果。
     */
    @PostMapping("/activate")
    public ActivatePluginResponse activatePlugin(@RequestBody ActivatePluginRequest request) {
        return harnessPluginCommandApi.activatePlugin(request);
    }

    /**
     * 运行 插件。
     * 流程：解析请求参数 → 调用应用门面 → 返回响应。
     * @param request 请求对象。
     * @return 方法执行结果。
     */
    @PostMapping("/run")
    public RunPluginResponse runPlugin(@RequestBody RunPluginRequest request) {
        return harnessPluginCommandApi.runPlugin(request);
    }

    /**
     * 停用 插件。
     * 流程：解析请求参数 → 调用应用门面 → 返回响应。
     * @param pluginId 插件 ID。
     * @return 方法执行结果。
     */
    @PostMapping("/{pluginId}/disable")
    public PluginLifecycleResponse disablePlugin(@PathVariable("pluginId") String pluginId) {
        return harnessPluginCommandApi.disablePlugin(pluginId);
    }

    /**
     * 启用 插件。
     * 流程：解析请求参数 → 调用应用门面 → 返回响应。
     * @param pluginId 插件 ID。
     * @return 方法执行结果。
     */
    @PostMapping("/{pluginId}/enable")
    public PluginLifecycleResponse enablePlugin(@PathVariable("pluginId") String pluginId) {
        return harnessPluginCommandApi.enablePlugin(pluginId);
    }

    /**
     * 卸载 插件。
     * 流程：解析请求参数 → 调用应用门面 → 返回响应。
     * @param pluginId 插件 ID。
     * @return 方法执行结果。
     */
    @PostMapping("/{pluginId}/uninstall")
    public PluginLifecycleResponse uninstallPlugin(@PathVariable("pluginId") String pluginId) {
        return harnessPluginCommandApi.uninstallPlugin(pluginId);
    }
}
