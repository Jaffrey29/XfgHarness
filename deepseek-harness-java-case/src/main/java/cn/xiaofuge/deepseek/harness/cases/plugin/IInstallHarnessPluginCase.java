package cn.xiaofuge.deepseek.harness.cases.plugin;

import cn.xiaofuge.deepseek.harness.cases.plugin.InstallPluginCommand;
import cn.xiaofuge.deepseek.harness.cases.plugin.InstallPluginResult;

/**
 * 编排插件安装用例。
 *
 * <p>调用流程：Controller/API → 插件用例 → 领域插件服务 → PluginRuntime/Bridge。</p>
 * <p>示例：安装插件后先激活，再通过统一工具名调用插件工具。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IInstallHarnessPluginCase {

    /**
     * 执行一次插件安装请求。
     *
     * @param request 安装请求
     * @return 安装结果
     */
    InstallPluginResult execute(InstallPluginCommand request);
}
