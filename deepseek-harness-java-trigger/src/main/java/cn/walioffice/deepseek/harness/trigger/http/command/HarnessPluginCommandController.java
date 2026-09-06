package cn.walioffice.deepseek.harness.trigger.http.command;

import cn.walioffice.deepseek.harness.api.dto.plugin.ActivatePluginRequest;
import cn.walioffice.deepseek.harness.api.dto.plugin.ActivatePluginResponse;
import cn.walioffice.deepseek.harness.api.dto.plugin.InstallPluginRequest;
import cn.walioffice.deepseek.harness.api.dto.plugin.InstallPluginResponse;
import cn.walioffice.deepseek.harness.api.dto.plugin.PluginLifecycleResponse;
import cn.walioffice.deepseek.harness.api.dto.plugin.RunPluginRequest;
import cn.walioffice.deepseek.harness.api.dto.plugin.RunPluginResponse;
import cn.walioffice.deepseek.harness.trigger.service.HarnessPluginCommandApi;
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

    public HarnessPluginCommandController(HarnessPluginCommandApi harnessPluginCommandApi) {
        this.harnessPluginCommandApi = harnessPluginCommandApi;
    }

    @PostMapping("/install")
    public InstallPluginResponse installPlugin(@RequestBody InstallPluginRequest request) {
        return harnessPluginCommandApi.installPlugin(request);
    }

    @PostMapping("/activate")
    public ActivatePluginResponse activatePlugin(@RequestBody ActivatePluginRequest request) {
        return harnessPluginCommandApi.activatePlugin(request);
    }

    @PostMapping("/run")
    public RunPluginResponse runPlugin(@RequestBody RunPluginRequest request) {
        return harnessPluginCommandApi.runPlugin(request);
    }

    @PostMapping("/{pluginId}/disable")
    public PluginLifecycleResponse disablePlugin(@PathVariable("pluginId") String pluginId) {
        return harnessPluginCommandApi.disablePlugin(pluginId);
    }

    @PostMapping("/{pluginId}/enable")
    public PluginLifecycleResponse enablePlugin(@PathVariable("pluginId") String pluginId) {
        return harnessPluginCommandApi.enablePlugin(pluginId);
    }

    @PostMapping("/{pluginId}/uninstall")
    public PluginLifecycleResponse uninstallPlugin(@PathVariable("pluginId") String pluginId) {
        return harnessPluginCommandApi.uninstallPlugin(pluginId);
    }
}
