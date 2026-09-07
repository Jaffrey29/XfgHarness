package cn.xiaofuge.deepseek.harness.cases.plugin.run;

import cn.xiaofuge.deepseek.harness.api.dto.RunPluginRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.RunPluginResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.plugin.IRunHarnessPluginCase;
import cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle.PluginRuntimeOutcome;
import cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle.PluginExecutionInput;
import cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle.PluginRuntimeKind;
import cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle.factory.PluginRuntimePipelineFactory;
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

    private final PluginRuntimePipelineFactory pipelineFactory;

    /**
     * 运行Harness Plugin Case Impl。
     */
    public RunHarnessPluginCaseImpl(
            PluginRuntimePipelineFactory pipelineFactory
    ) {
        this.pipelineFactory = pipelineFactory;
    }

    /**
     * 用例执行入口，组装 Pipeline 并返回处理结果。
     */
    @Override
    public RunPluginResponseDTO execute(RunPluginRequestDTO request) {
        if (pipelineFactory.pluginPipeline().execute(new PluginExecutionInput(request.pluginId(), PluginRuntimeKind.RUN)) instanceof PluginRuntimeOutcome.Run run) {
            return run.result();
        }
        throw new IllegalStateException("unexpected plugin run outcome");
    }
}
