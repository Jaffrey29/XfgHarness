package cn.xiaofuge.deepseek.harness.cases.plugin;

import java.time.Instant;

/**
 * 插件生命周期操作结果，包括启用、停用和卸载。
 *
 * @param pluginId 插件 ID
 * @param status 生命周期操作后的状态
 * @param message 面向使用者的提示
 * @param updatedAt 状态变更时间
 *
 * <p>调用流程：Controller/API → 插件用例 → 领域插件服务 → PluginRuntime/Bridge。</p>
 * <p>示例：安装插件后先激活，再通过统一工具名调用插件工具。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public record PluginLifecycleResult(
        String pluginId,
        String status,
        String message,
        Instant updatedAt
) {
}
