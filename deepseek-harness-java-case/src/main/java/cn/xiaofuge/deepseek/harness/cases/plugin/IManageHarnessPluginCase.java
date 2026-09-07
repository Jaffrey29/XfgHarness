package cn.xiaofuge.deepseek.harness.cases.plugin;

import cn.xiaofuge.deepseek.harness.cases.plugin.PluginLifecycleResult;

/**
 * 编排插件生命周期管理，包括启用、停用、卸载和停止。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IManageHarnessPluginCase {

    PluginLifecycleResult disable(String pluginId);

    PluginLifecycleResult enable(String pluginId);

    PluginLifecycleResult uninstall(String pluginId);

    PluginLifecycleResult stop(String pluginId);
}
