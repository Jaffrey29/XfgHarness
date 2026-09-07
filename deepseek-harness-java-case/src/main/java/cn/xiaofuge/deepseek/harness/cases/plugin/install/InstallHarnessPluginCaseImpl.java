package cn.xiaofuge.deepseek.harness.cases.plugin.install;

import cn.xiaofuge.deepseek.harness.cases.plugin.InstallPluginCommand;
import cn.xiaofuge.deepseek.harness.cases.plugin.InstallPluginResult;
import cn.xiaofuge.deepseek.harness.cases.plugin.IInstallHarnessPluginCase;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.valobj.PluginInstallationResultVO;
import cn.xiaofuge.deepseek.harness.domain.plugin.IPluginRegistryService;
import org.springframework.stereotype.Service;

/**
 * 协调插件安装纵切面。
 *
 * <p>调用流程：Controller/API → 插件用例 → 领域插件服务 → PluginRuntime/Bridge。</p>
 * <p>示例：安装插件后先激活，再通过统一工具名调用插件工具。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class InstallHarnessPluginCaseImpl implements IInstallHarnessPluginCase {

    private final IPluginRegistryService pluginRegistryService;

    /**
     * 安装组件，写入登记信息并返回安装结果。
     */
    public InstallHarnessPluginCaseImpl(IPluginRegistryService pluginRegistryService) {
        this.pluginRegistryService = pluginRegistryService;
    }

    @Override
    public InstallPluginResult execute(InstallPluginCommand request) {
        PluginInstallationResultVO result = pluginRegistryService.installPlugin(
                request.pluginId(),
                request.displayName(),
                request.pluginVersion(),
                request.runtimeType(),
                request.sourcePath(),
                request.entrypoint()
        );
        return new InstallPluginResult(
                result.pluginId(),
                result.runtimeType().name(),
                result.installMode().name(),
                result.status().name(),
                result.directlyExecutable(),
                result.installedAt()
        );
    }
}
