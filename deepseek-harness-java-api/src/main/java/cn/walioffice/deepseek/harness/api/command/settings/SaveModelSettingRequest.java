package cn.walioffice.deepseek.harness.api.command.settings;

/**
 * 承载用户编辑后的一条模型设置记录。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record SaveModelSettingRequest(
        String profileCode,
        String providerCode,
        String modelCode,
        String baseUrl,
        String apiKeyRef,
        boolean enabled
) {
}
