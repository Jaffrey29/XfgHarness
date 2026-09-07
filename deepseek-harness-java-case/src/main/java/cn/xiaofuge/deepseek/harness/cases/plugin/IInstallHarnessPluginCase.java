package cn.xiaofuge.deepseek.harness.cases.plugin;

import cn.xiaofuge.deepseek.harness.cases.plugin.InstallPluginCommand;
import cn.xiaofuge.deepseek.harness.cases.plugin.InstallPluginResult;

/**
 * 编排插件安装用例。
 *
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
