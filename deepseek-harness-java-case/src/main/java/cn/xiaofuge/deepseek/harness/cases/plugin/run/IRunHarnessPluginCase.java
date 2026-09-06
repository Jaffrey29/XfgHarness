package cn.xiaofuge.deepseek.harness.cases.plugin.run;

import cn.xiaofuge.deepseek.harness.api.dto.plugin.RunPluginRequest;
import cn.xiaofuge.deepseek.harness.api.dto.plugin.RunPluginResponse;

/**
 * 编排插件运行用例。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IRunHarnessPluginCase {

    RunPluginResponse execute(RunPluginRequest request);
}
