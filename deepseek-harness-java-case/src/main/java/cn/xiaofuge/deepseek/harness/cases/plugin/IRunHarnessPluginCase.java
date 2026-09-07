package cn.xiaofuge.deepseek.harness.cases.plugin;

import cn.xiaofuge.deepseek.harness.cases.plugin.RunPluginCommand;
import cn.xiaofuge.deepseek.harness.cases.plugin.RunPluginResult;

/**
 * 编排插件运行用例。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IRunHarnessPluginCase {

    RunPluginResult execute(RunPluginCommand request);
}
