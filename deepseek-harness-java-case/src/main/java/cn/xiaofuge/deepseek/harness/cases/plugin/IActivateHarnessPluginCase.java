package cn.xiaofuge.deepseek.harness.cases.plugin;

import cn.xiaofuge.deepseek.harness.api.dto.ActivatePluginRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.ActivatePluginResponseDTO;

/**
 * 编排插件激活用例。
 *
 * <p>调用流程：Controller/API → 插件用例 → 领域插件服务 → PluginRuntime/Bridge。</p>
 * <p>示例：安装插件后先激活，再通过统一工具名调用插件工具。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IActivateHarnessPluginCase {

    /**
     * 执行一次插件激活请求。
     *
     * @param request 激活请求
     * @return 激活结果
     */
    ActivatePluginResponseDTO execute(ActivatePluginRequestDTO request);
}
