package cn.xiaofuge.deepseek.harness.cases.plugin.manage;

import cn.xiaofuge.deepseek.harness.api.dto.PluginLifecycleResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.plugin.IManageHarnessPluginCase;
import cn.xiaofuge.deepseek.harness.cases.plugin.manage.factory.PluginManagePipelineFactory;
import cn.xiaofuge.deepseek.harness.cases.plugin.manage.node.ManagePluginNode;
import org.springframework.stereotype.Service;

/**
 * 协调插件生命周期管理纵切面。
 *
 * <p>调用流程：Controller/API → 插件用例 → 领域插件服务 → PluginRuntime/Bridge。</p>
 * <p>示例：安装插件后先激活，再通过统一工具名调用插件工具。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class ManageHarnessPluginCaseImpl implements IManageHarnessPluginCase {

    private final PluginManagePipelineFactory pluginManagePipelineFactory;

    /**
     * 初始化插件生命周期管理用例。
     * <p>流程：接收启用/停用/卸载命令 → 委托插件领域服务执行。</p>
     */
    public ManageHarnessPluginCaseImpl(
            PluginManagePipelineFactory pluginManagePipelineFactory
    ) {
        this.pluginManagePipelineFactory = pluginManagePipelineFactory;
    }

    /**
     * enable。
     * 流程：读取命令 → 执行用例编排 → 返回结果。
     * @param pluginId 插件 ID。
     * @return 方法执行结果。
     */

    @Override
    public PluginLifecycleResponseDTO disable(String pluginId) {
        return execute(ManagePluginNode.Action.DISABLE, pluginId);
    }

    /**
     * uninstall。
     * 流程：读取命令 → 执行用例编排 → 返回结果。
     * @param pluginId 插件 ID。
     * @return 方法执行结果。
     */

    @Override
    public PluginLifecycleResponseDTO enable(String pluginId) {
        return execute(ManagePluginNode.Action.ENABLE, pluginId);
    }

    /**
     * 停止目标对象。
     * 流程：读取命令 → 执行用例编排 → 返回结果。
     * @param pluginId 插件 ID。
     * @return 方法执行结果。
     */

    @Override
    public PluginLifecycleResponseDTO uninstall(String pluginId) {
        return execute(ManagePluginNode.Action.UNINSTALL, pluginId);
    }

    @Override
    public PluginLifecycleResponseDTO stop(String pluginId) {
        return execute(ManagePluginNode.Action.STOP, pluginId);
    }

    private PluginLifecycleResponseDTO execute(ManagePluginNode.Action action, String pluginId) {
        return pluginManagePipelineFactory.pipeline().execute(new ManagePluginNode.Input(action, pluginId));
    }
}
