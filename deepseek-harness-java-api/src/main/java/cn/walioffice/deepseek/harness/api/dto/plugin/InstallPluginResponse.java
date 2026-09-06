package cn.walioffice.deepseek.harness.api.dto.plugin;

import java.time.Instant;

/**
 * 插件安装响应。
 *
 * @param pluginId 插件 ID
 * @param runtimeType 插件运行时类型
 * @param installMode 解析后的安装模式
 * @param status 安装状态
 * @param directlyExecutable 是否为 JVM 原生插件
 * @param installedAt 注册时间
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record InstallPluginResponse(
        String pluginId,
        String runtimeType,
        String installMode,
        String status,
        boolean directlyExecutable,
        Instant installedAt
) {
}
