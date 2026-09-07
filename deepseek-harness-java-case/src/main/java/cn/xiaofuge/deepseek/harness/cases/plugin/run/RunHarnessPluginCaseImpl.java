package cn.xiaofuge.deepseek.harness.cases.plugin.run;

import cn.xiaofuge.deepseek.harness.cases.plugin.RunPluginCommand;
import cn.xiaofuge.deepseek.harness.cases.plugin.RunPluginResult;
import cn.xiaofuge.deepseek.harness.cases.plugin.IRunHarnessPluginCase;
import cn.xiaofuge.deepseek.harness.domain.plugin.bridge.model.valobj.PluginBridgePlanVO;
import cn.xiaofuge.deepseek.harness.domain.plugin.IPluginBridgeService;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.entity.HarnessPluginEntity;
import cn.xiaofuge.deepseek.harness.domain.plugin.IPluginRegistryService;
import cn.xiaofuge.deepseek.harness.domain.plugin.runtime.model.valobj.PluginRunResultVO;
import cn.xiaofuge.deepseek.harness.domain.plugin.IPluginRuntimeService;
import java.time.Instant;
import org.springframework.stereotype.Service;

/**
 * 协调插件进程启动。
 *
 * <p>调用流程：Controller/API → 插件用例 → 领域插件服务 → PluginRuntime/Bridge。</p>
 * <p>示例：安装插件后先激活，再通过统一工具名调用插件工具。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class RunHarnessPluginCaseImpl implements IRunHarnessPluginCase {

    private final IPluginRegistryService pluginRegistryService;
    private final IPluginBridgeService pluginBridgeService;
    private final IPluginRuntimeService pluginRuntimeService;

    /**
     * 运行Harness Plugin Case Impl。
     */
    public RunHarnessPluginCaseImpl(
            IPluginRegistryService pluginRegistryService,
            IPluginBridgeService pluginBridgeService,
            IPluginRuntimeService pluginRuntimeService
    ) {
        this.pluginRegistryService = pluginRegistryService;
        this.pluginBridgeService = pluginBridgeService;
        this.pluginRuntimeService = pluginRuntimeService;
    }

    @Override
    public RunPluginResult execute(RunPluginCommand request) {
        HarnessPluginEntity plugin = pluginRegistryService.loadPlugin(request.pluginId());
        PluginBridgePlanVO plan = pluginBridgeService.inspect(plugin);
        pluginRegistryService.refreshStatus(plugin, plan.bridgeReady());
        pluginBridgeService.recordBinding(plugin.getPluginId(), plan, Instant.now());
        if (!plan.bridgeReady()) {
            throw new IllegalStateException("plugin bridge is not ready: " + plan.message());
        }
        PluginRunResultVO result = pluginRuntimeService.start(plugin, plan);
        return new RunPluginResult(
                result.pluginId(),
                result.processId(),
                result.runtimeCommand(),
                result.workingDirectory(),
                result.running(),
                result.startedAt(),
                result.message()
        );
    }
}
