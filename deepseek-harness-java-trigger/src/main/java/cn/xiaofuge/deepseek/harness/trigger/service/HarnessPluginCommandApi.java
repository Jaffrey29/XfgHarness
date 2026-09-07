package cn.xiaofuge.deepseek.harness.trigger.service;

import cn.xiaofuge.deepseek.harness.api.dto.plugin.ActivatePluginRequest;
import cn.xiaofuge.deepseek.harness.api.dto.plugin.ActivatePluginResponse;
import cn.xiaofuge.deepseek.harness.api.dto.plugin.InstallPluginRequest;
import cn.xiaofuge.deepseek.harness.api.dto.plugin.InstallPluginResponse;
import cn.xiaofuge.deepseek.harness.api.dto.plugin.PluginLifecycleResponse;
import cn.xiaofuge.deepseek.harness.api.dto.plugin.RunPluginRequest;
import cn.xiaofuge.deepseek.harness.api.dto.plugin.RunPluginResponse;
import cn.xiaofuge.deepseek.harness.cases.plugin.ActivatePluginCommand;
import cn.xiaofuge.deepseek.harness.cases.plugin.ActivatePluginResult;
import cn.xiaofuge.deepseek.harness.cases.plugin.IActivateHarnessPluginCase;
import cn.xiaofuge.deepseek.harness.cases.plugin.IInstallHarnessPluginCase;
import cn.xiaofuge.deepseek.harness.cases.plugin.IManageHarnessPluginCase;
import cn.xiaofuge.deepseek.harness.cases.plugin.IRunHarnessPluginCase;
import cn.xiaofuge.deepseek.harness.cases.plugin.InstallPluginCommand;
import cn.xiaofuge.deepseek.harness.cases.plugin.InstallPluginResult;
import cn.xiaofuge.deepseek.harness.cases.plugin.PluginLifecycleResult;
import cn.xiaofuge.deepseek.harness.cases.plugin.RunPluginCommand;
import cn.xiaofuge.deepseek.harness.cases.plugin.RunPluginResult;
import org.springframework.stereotype.Service;

/**
 * 插件命令适配器。
 * <p>
 * 执行流程：
 * 1. 接收 Trigger 层传入的插件命令请求（api DTO）；
 * 2. 映射为 case 层 Command，转交对应的用例层执行；
 * 3. 将 case 层 Result 映射回 api Response 返回给 Trigger 层输出。
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
        InstallPluginCommand cmd = new InstallPluginCommand(
                request.pluginId(), request.displayName(), request.pluginVersion(),
                request.runtimeType(), request.sourcePath(), request.entrypoint());
        InstallPluginResult r = installHarnessPluginCase.execute(cmd);
        return new InstallPluginResponse(
                r.pluginId(), r.runtimeType(), r.installMode(),
                r.status(), r.directlyExecutable(), r.installedAt());
    }

    public ActivatePluginResponse activatePlugin(ActivatePluginRequest request) {
        ActivatePluginCommand cmd = new ActivatePluginCommand(request.pluginId());
        ActivatePluginResult r = activateHarnessPluginCase.execute(cmd);
        return new ActivatePluginResponse(
                r.pluginId(), r.status(), r.packageType(),
                r.runtimeCommand(), r.bridgeReady(), r.message(), r.checkedAt());
    }

    public RunPluginResponse runPlugin(RunPluginRequest request) {
        RunPluginCommand cmd = new RunPluginCommand(request.pluginId());
        RunPluginResult r = runHarnessPluginCase.execute(cmd);
        return new RunPluginResponse(
                r.pluginId(), r.processId(), r.runtimeCommand(),
                r.workingDirectory(), r.running(), r.startedAt(), r.message());
    }

    public PluginLifecycleResponse disablePlugin(String pluginId) {
        PluginLifecycleResult r = manageHarnessPluginCase.disable(pluginId);
        return new PluginLifecycleResponse(r.pluginId(), r.status(), r.message(), r.updatedAt());
    }

    public PluginLifecycleResponse enablePlugin(String pluginId) {
        PluginLifecycleResult r = manageHarnessPluginCase.enable(pluginId);
        return new PluginLifecycleResponse(r.pluginId(), r.status(), r.message(), r.updatedAt());
    }

    public PluginLifecycleResponse uninstallPlugin(String pluginId) {
        PluginLifecycleResult r = manageHarnessPluginCase.uninstall(pluginId);
        return new PluginLifecycleResponse(r.pluginId(), r.status(), r.message(), r.updatedAt());
    }
}
