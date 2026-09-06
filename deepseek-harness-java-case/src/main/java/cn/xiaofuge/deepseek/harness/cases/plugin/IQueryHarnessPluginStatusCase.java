package cn.xiaofuge.deepseek.harness.cases.plugin;

import cn.xiaofuge.deepseek.harness.api.dto.plugin.QueryPluginStatusResponse;

/**
 * 编排插件进程状态查询。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IQueryHarnessPluginStatusCase {

    QueryPluginStatusResponse execute(String pluginId);
}
