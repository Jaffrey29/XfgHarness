package cn.walioffice.deepseek.harness.cases.plugin.manage;

import cn.walioffice.deepseek.harness.api.dto.plugin.PluginLifecycleResponse;

/**
 * 编排插件生命周期管理，包括启用、停用、卸载和停止。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IManageHarnessPluginCase {

    PluginLifecycleResponse disable(String pluginId);

    PluginLifecycleResponse enable(String pluginId);

    PluginLifecycleResponse uninstall(String pluginId);

    PluginLifecycleResponse stop(String pluginId);
}
