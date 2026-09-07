package cn.xiaofuge.deepseek.harness.trigger.http.command;

import cn.xiaofuge.deepseek.harness.api.dto.ActivatePluginRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.ActivatePluginResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.InstallPluginRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.InstallPluginResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.PluginLifecycleResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.RunPluginRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.RunPluginResponseDTO;
import cn.xiaofuge.deepseek.harness.api.IHarnessPluginCommandApi;
import cn.xiaofuge.deepseek.harness.api.response.Response;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 面向 Web 控制台暴露插件写操作。 */
@RestController
@RequestMapping("/api/harness/plugins")
public class HarnessPluginCommandController {

    private final IHarnessPluginCommandApi harnessPluginCommandApi;

    /**
     * 处理Harness 插件 命令 Controller。
     * 流程：解析请求参数 → 调用应用门面 → 返回响应。
     * @param harnessPluginCommandApi harness 插件 命令 API参数。
     * @return 方法执行结果。
     */
    public HarnessPluginCommandController(IHarnessPluginCommandApi harnessPluginCommandApi) {
        this.harnessPluginCommandApi = harnessPluginCommandApi;
    }

    /**
     * 安装 插件。
     * 流程：解析请求参数 → 调用应用门面 → 返回响应。
     * @param request 请求对象。
     * @return 方法执行结果。
     */
    @PostMapping("/install")
    public Response<InstallPluginResponseDTO> installPlugin(@RequestBody InstallPluginRequestDTO request) {
        return harnessPluginCommandApi.installPlugin(request);
    }

    /**
     * 激活 插件。
     * 流程：解析请求参数 → 调用应用门面 → 返回响应。
     * @param request 请求对象。
     * @return 方法执行结果。
     */
    @PostMapping("/activate")
    public Response<ActivatePluginResponseDTO> activatePlugin(@RequestBody ActivatePluginRequestDTO request) {
        return harnessPluginCommandApi.activatePlugin(request);
    }

    /**
     * 运行 插件。
     * 流程：解析请求参数 → 调用应用门面 → 返回响应。
     * @param request 请求对象。
     * @return 方法执行结果。
     */
    @PostMapping("/run")
    public Response<RunPluginResponseDTO> runPlugin(@RequestBody RunPluginRequestDTO request) {
        return harnessPluginCommandApi.runPlugin(request);
    }

    /**
     * 停用 插件。
     * 流程：解析请求参数 → 调用应用门面 → 返回响应。
     * @param pluginId 插件 ID。
     * @return 方法执行结果。
     */
    @PostMapping("/{pluginId}/disable")
    public Response<PluginLifecycleResponseDTO> disablePlugin(@PathVariable("pluginId") String pluginId) {
        return harnessPluginCommandApi.disablePlugin(pluginId);
    }

    /**
     * 启用 插件。
     * 流程：解析请求参数 → 调用应用门面 → 返回响应。
     * @param pluginId 插件 ID。
     * @return 方法执行结果。
     */
    @PostMapping("/{pluginId}/enable")
    public Response<PluginLifecycleResponseDTO> enablePlugin(@PathVariable("pluginId") String pluginId) {
        return harnessPluginCommandApi.enablePlugin(pluginId);
    }

    /**
     * 卸载 插件。
     * 流程：解析请求参数 → 调用应用门面 → 返回响应。
     * @param pluginId 插件 ID。
     * @return 方法执行结果。
     */
    @PostMapping("/{pluginId}/uninstall")
    public Response<PluginLifecycleResponseDTO> uninstallPlugin(@PathVariable("pluginId") String pluginId) {
        return harnessPluginCommandApi.uninstallPlugin(pluginId);
    }
}
