package cn.xiaofuge.deepseek.harness.cases.plugin.install;

import cn.xiaofuge.deepseek.harness.api.dto.plugin.InstallPluginRequest;
import cn.xiaofuge.deepseek.harness.api.dto.plugin.InstallPluginResponse;
import cn.xiaofuge.deepseek.harness.cases.plugin.IInstallHarnessPluginCase;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.valobj.PluginInstallationResultVO;
import cn.xiaofuge.deepseek.harness.domain.plugin.IPluginRegistryService;
import org.springframework.stereotype.Service;

/**
 * 协调插件安装纵切面。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class InstallHarnessPluginCaseImpl implements IInstallHarnessPluginCase {

    private final IPluginRegistryService pluginRegistryService;

    public InstallHarnessPluginCaseImpl(IPluginRegistryService pluginRegistryService) {
        this.pluginRegistryService = pluginRegistryService;
    }

    @Override
    public InstallPluginResponse execute(InstallPluginRequest request) {
        PluginInstallationResultVO result = pluginRegistryService.installPlugin(
                request.pluginId(),
                request.displayName(),
                request.pluginVersion(),
                request.runtimeType(),
                request.sourcePath(),
                request.entrypoint()
        );
        return new InstallPluginResponse(
                result.pluginId(),
                result.runtimeType().name(),
                result.installMode().name(),
                result.status().name(),
                result.directlyExecutable(),
                result.installedAt()
        );
    }
}
