package cn.walioffice.deepseek.harness.cases.plugin.activate;

import cn.walioffice.deepseek.harness.api.dto.plugin.ActivatePluginRequest;
import cn.walioffice.deepseek.harness.api.dto.plugin.ActivatePluginResponse;

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
    ActivatePluginResponse execute(ActivatePluginRequest request);
}
