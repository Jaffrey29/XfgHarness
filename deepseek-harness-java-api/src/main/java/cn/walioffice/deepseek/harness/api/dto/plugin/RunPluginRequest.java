package cn.walioffice.deepseek.harness.api.dto.plugin;

/**
 * 插件进程启动请求。
 *
 * @param pluginId 插件 ID
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record RunPluginRequest(String pluginId) {
}
