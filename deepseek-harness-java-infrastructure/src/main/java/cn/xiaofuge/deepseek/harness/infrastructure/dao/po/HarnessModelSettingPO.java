package cn.xiaofuge.deepseek.harness.infrastructure.dao.po;

import java.time.Instant;

/**
 * 保存一条持久化的模型设置记录。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record HarnessModelSettingPO(
        String profileCode,
        String providerCode,
        String modelCode,
        String baseUrl,
        String apiKeyRef,
        boolean enabled,
        Instant updatedAt
) {
}
