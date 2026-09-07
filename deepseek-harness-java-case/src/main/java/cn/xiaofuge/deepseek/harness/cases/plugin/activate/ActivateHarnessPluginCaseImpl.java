package cn.xiaofuge.deepseek.harness.cases.plugin.activate;

import cn.xiaofuge.deepseek.harness.api.dto.ActivatePluginRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.ActivatePluginResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.plugin.IActivateHarnessPluginCase;
import cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle.PluginRuntimeOutcome;
import cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle.PluginExecutionInput;
import cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle.PluginRuntimeKind;
import cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle.factory.PluginRuntimePipelineFactory;
import org.springframework.stereotype.Service;

/**
 * 协调插件激活纵切面。
 *
 * <p>调用流程：Controller/API → 插件用例 → 领域插件服务 → PluginRuntime/Bridge。</p>
 * <p>示例：安装插件后先激活，再通过统一工具名调用插件工具。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class ActivateHarnessPluginCaseImpl implements IActivateHarnessPluginCase {

    private final PluginRuntimePipelineFactory pipelineFactory;

    /**
     * 激活组件，准备其运行时能力。
     */
    public ActivateHarnessPluginCaseImpl(
            PluginRuntimePipelineFactory pipelineFactory
    ) {
        this.pipelineFactory = pipelineFactory;
    }

    @Override
    public ActivatePluginResponseDTO execute(ActivatePluginRequestDTO request) {
        if (pipelineFactory.pluginPipeline().execute(new PluginExecutionInput(request.pluginId(), PluginRuntimeKind.ACTIVATE))
                instanceof PluginRuntimeOutcome.Activation activation) {
            return activation.result();
        }
        throw new IllegalStateException("unexpected plugin activation outcome");
    }
}
