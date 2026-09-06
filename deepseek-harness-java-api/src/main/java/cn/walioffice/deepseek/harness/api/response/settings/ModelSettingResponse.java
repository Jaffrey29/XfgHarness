package cn.walioffice.deepseek.harness.api.response.settings;

import java.time.Instant;

/**
 * 返回一条用户可见的模型设置记录。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record ModelSettingResponse(
        String profileCode,
        String providerCode,
        String modelCode,
        String baseUrl,
        String apiKeyRef,
        boolean enabled,
        Instant updatedAt
) {
}
