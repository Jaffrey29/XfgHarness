package cn.xiaofuge.deepseek.harness.cases.plugin;

import cn.xiaofuge.deepseek.harness.cases.plugin.ActivatePluginCommand;
import cn.xiaofuge.deepseek.harness.cases.plugin.ActivatePluginResult;

/**
 * 编排插件激活用例。
 *
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
    ActivatePluginResult execute(ActivatePluginCommand request);
}
