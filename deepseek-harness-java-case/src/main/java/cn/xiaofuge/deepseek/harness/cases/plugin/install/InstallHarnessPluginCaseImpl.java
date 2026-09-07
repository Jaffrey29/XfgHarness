package cn.xiaofuge.deepseek.harness.cases.plugin.install;

import cn.xiaofuge.deepseek.harness.api.dto.InstallPluginRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.InstallPluginResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.plugin.IInstallHarnessPluginCase;
import cn.xiaofuge.deepseek.harness.cases.plugin.install.factory.PluginInstallPipelineFactory;
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

    private final PluginInstallPipelineFactory pluginInstallPipelineFactory;

    /**
     * 安装组件，写入登记信息并返回安装结果。
     */
    public InstallHarnessPluginCaseImpl(PluginInstallPipelineFactory pluginInstallPipelineFactory) {
        this.pluginInstallPipelineFactory = pluginInstallPipelineFactory;
    }

    /**
     * 用例执行入口，组装 Pipeline 并返回处理结果。
     */
    @Override
    public InstallPluginResponseDTO execute(InstallPluginRequestDTO request) {
        return pluginInstallPipelineFactory.pipeline().execute(request);
    }
}
