package cn.xiaofuge.deepseek.harness.trigger.service;

import cn.xiaofuge.deepseek.harness.api.dto.plugin.ActivatePluginRequest;
import cn.xiaofuge.deepseek.harness.api.dto.plugin.ActivatePluginResponse;
import cn.xiaofuge.deepseek.harness.api.dto.plugin.InstallPluginRequest;
import cn.xiaofuge.deepseek.harness.api.dto.plugin.InstallPluginResponse;
import cn.xiaofuge.deepseek.harness.api.dto.plugin.PluginLifecycleResponse;
import cn.xiaofuge.deepseek.harness.api.dto.plugin.RunPluginRequest;
import cn.xiaofuge.deepseek.harness.api.dto.plugin.RunPluginResponse;
import cn.xiaofuge.deepseek.harness.cases.plugin.IActivateHarnessPluginCase;
import cn.xiaofuge.deepseek.harness.cases.plugin.IInstallHarnessPluginCase;
import cn.xiaofuge.deepseek.harness.cases.plugin.IManageHarnessPluginCase;
import cn.xiaofuge.deepseek.harness.cases.plugin.IRunHarnessPluginCase;
import org.springframework.stereotype.Service;

/**
 * 插件命令适配器。
 * <p>
 * 执行流程：
 * 1. 接收 Trigger 层传入的插件命令请求；
 * 2. 转交对应的用例层执行安装、激活、运行或生命周期管理；
 * 3. 将结果直接返回给 Trigger 层输出。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class HarnessPluginCommandApi {

    private final IInstallHarnessPluginCase installHarnessPluginCase;
    private final IActivateHarnessPluginCase activateHarnessPluginCase;
    private final IRunHarnessPluginCase runHarnessPluginCase;
    private final IManageHarnessPluginCase manageHarnessPluginCase;

    public HarnessPluginCommandApi(
            IInstallHarnessPluginCase installHarnessPluginCase,
            IActivateHarnessPluginCase activateHarnessPluginCase,
            IRunHarnessPluginCase runHarnessPluginCase,
            IManageHarnessPluginCase manageHarnessPluginCase
    ) {
        this.installHarnessPluginCase = installHarnessPluginCase;
        this.activateHarnessPluginCase = activateHarnessPluginCase;
        this.runHarnessPluginCase = runHarnessPluginCase;
        this.manageHarnessPluginCase = manageHarnessPluginCase;
    }

    public InstallPluginResponse installPlugin(InstallPluginRequest request) {
        return installHarnessPluginCase.execute(request);
    }

    public ActivatePluginResponse activatePlugin(ActivatePluginRequest request) {
        return activateHarnessPluginCase.execute(request);
    }

    public RunPluginResponse runPlugin(RunPluginRequest request) {
        return runHarnessPluginCase.execute(request);
    }

    public PluginLifecycleResponse disablePlugin(String pluginId) {
        return manageHarnessPluginCase.disable(pluginId);
    }

    public PluginLifecycleResponse enablePlugin(String pluginId) {
        return manageHarnessPluginCase.enable(pluginId);
    }

    public PluginLifecycleResponse uninstallPlugin(String pluginId) {
        return manageHarnessPluginCase.uninstall(pluginId);
    }
}
