package cn.xiaofuge.deepseek.harness.api.dto.plugin;

import java.time.Instant;

/**
 * 插件生命周期操作响应，包括启用、停用和卸载。
 *
 * @param pluginId 插件 ID
 * @param status 生命周期操作后的状态
 * @param message 面向使用者的提示
 * @param updatedAt 状态变更时间
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record PluginLifecycleResponse(
        String pluginId,
        String status,
        String message,
        Instant updatedAt
) {
}
