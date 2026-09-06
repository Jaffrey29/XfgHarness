package cn.walioffice.deepseek.harness.cases.plugin.run;

import cn.walioffice.deepseek.harness.api.dto.plugin.QueryPluginStatusResponse;

/**
 * 编排插件进程状态查询。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IQueryHarnessPluginStatusCase {

    QueryPluginStatusResponse execute(String pluginId);
}
