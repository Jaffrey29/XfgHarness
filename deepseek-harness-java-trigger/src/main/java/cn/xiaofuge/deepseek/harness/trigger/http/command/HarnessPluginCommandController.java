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
     * 处理Harness Plugin Command Controller相关逻辑。
     */
    public HarnessPluginCommandController(HarnessPluginCommandApi harnessPluginCommandApi) {
        this.harnessPluginCommandApi = harnessPluginCommandApi;
    }

    /**
     * 处理Post Mapping相关逻辑。
     */
    @PostMapping("/install")
    public InstallPluginResponse installPlugin(@RequestBody InstallPluginRequest request) {
        return harnessPluginCommandApi.installPlugin(request);
    }

    /**
     * 处理Post Mapping相关逻辑。
     */
    @PostMapping("/activate")
    public ActivatePluginResponse activatePlugin(@RequestBody ActivatePluginRequest request) {
        return harnessPluginCommandApi.activatePlugin(request);
    }

    /**
     * 处理Post Mapping相关逻辑。
     */
    @PostMapping("/run")
    public RunPluginResponse runPlugin(@RequestBody RunPluginRequest request) {
        return harnessPluginCommandApi.runPlugin(request);
    }

    /**
     * 处理Post Mapping相关逻辑。
     */
    @PostMapping("/{pluginId}/disable")
    public PluginLifecycleResponse disablePlugin(@PathVariable("pluginId") String pluginId) {
        return harnessPluginCommandApi.disablePlugin(pluginId);
    }

    /**
     * 处理Post Mapping相关逻辑。
     */
    @PostMapping("/{pluginId}/enable")
    public PluginLifecycleResponse enablePlugin(@PathVariable("pluginId") String pluginId) {
        return harnessPluginCommandApi.enablePlugin(pluginId);
    }

    /**
     * 处理Post Mapping相关逻辑。
     */
    @PostMapping("/{pluginId}/uninstall")
    public PluginLifecycleResponse uninstallPlugin(@PathVariable("pluginId") String pluginId) {
        return harnessPluginCommandApi.uninstallPlugin(pluginId);
    }
}
