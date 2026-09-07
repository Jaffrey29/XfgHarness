package cn.xiaofuge.deepseek.harness.cases.plugin;

import java.time.Instant;

/**
 * 插件激活结果。
 *
 * @param pluginId 插件 ID
 * @param status 激活后的状态
 * @param packageType 识别出的包类型
 * @param runtimeCommand 解析后的运行命令
 * @param bridgeReady 桥接层是否可以执行插件
 * @param message 面向使用者的激活提示
 * @param checkedAt 激活检查时间
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record ActivatePluginResult(
        String pluginId,
        String status,
        String packageType,
        String runtimeCommand,
        boolean bridgeReady,
        String message,
        Instant checkedAt
) {
}
