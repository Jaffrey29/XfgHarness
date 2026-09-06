package cn.xiaofuge.deepseek.harness.cases.plugin.install;

import cn.xiaofuge.deepseek.harness.api.dto.plugin.InstallPluginRequest;
import cn.xiaofuge.deepseek.harness.api.dto.plugin.InstallPluginResponse;

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
    InstallPluginResponse execute(InstallPluginRequest request);
}
