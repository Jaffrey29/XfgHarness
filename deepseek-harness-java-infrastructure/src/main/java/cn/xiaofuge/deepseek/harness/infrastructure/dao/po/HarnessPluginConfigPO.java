package cn.xiaofuge.deepseek.harness.infrastructure.dao.po;

import java.time.Instant;

/**
 * 保存一条分层插件配置。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record HarnessPluginConfigPO(
        String pluginId,
        String configKey,
        String configValue,
        String configLayer,
        Instant updatedAt
) {
}
