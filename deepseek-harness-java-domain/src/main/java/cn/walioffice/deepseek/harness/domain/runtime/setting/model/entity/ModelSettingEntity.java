package cn.walioffice.deepseek.harness.domain.runtime.setting.model.entity;

import java.time.Instant;
import java.util.Objects;

/**
 * 模型设置实体。
 * <p>
 * 执行流程：
 * 1. 领域层创建或重配模型设置；
 * 2. 实体负责约束必填字段和更新时间；
 * 3. 仓储将实体持久化到数据库或配置源。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class ModelSettingEntity {

    private final String profileCode;
    private final String providerCode;
    private final String modelCode;
    private final String baseUrl;
    private final String apiKeyRef;
    private final boolean enabled;
    private final Instant updatedAt;

    public ModelSettingEntity(
            String profileCode,
            String providerCode,
            String modelCode,
            String baseUrl,
            String apiKeyRef,
            boolean enabled,
            Instant updatedAt
    ) {
        this.profileCode = Objects.requireNonNull(profileCode, "profileCode");
        this.providerCode = Objects.requireNonNull(providerCode, "providerCode");
        this.modelCode = Objects.requireNonNull(modelCode, "modelCode");
        this.baseUrl = baseUrl;
        this.apiKeyRef = apiKeyRef;
        this.enabled = enabled;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    /**
     * 创建一个新的模型设置实体。
     * <p>
     * 执行流程：
     * 1. 校验必填字段；
     * 2. 规范化空白字符串；
     * 3. 生成带当前时间戳的实体对象。
     */
    public static ModelSettingEntity create(
            String profileCode,
            String providerCode,
            String modelCode,
            String baseUrl,
            String apiKeyRef,
            boolean enabled
    ) {
        return new ModelSettingEntity(
                requireText(profileCode, "profileCode"),
                requireText(providerCode, "providerCode"),
                requireText(modelCode, "modelCode"),
                blankToNull(baseUrl),
                blankToNull(apiKeyRef),
                enabled,
                Instant.now()
        );
    }

    /**
     * 重新配置当前模型设置。
     * <p>
     * 执行流程：
     * 1. 保留原有 profileCode；
     * 2. 更新提供方、模型和访问参数；
     * 3. 使用新的更新时间生成新实体。
     */
    public ModelSettingEntity reconfigure(
            String providerCode,
            String modelCode,
            String baseUrl,
            String apiKeyRef,
            boolean enabled,
            Instant updatedAt
    ) {
        return new ModelSettingEntity(
                profileCode,
                requireText(providerCode, "providerCode"),
                requireText(modelCode, "modelCode"),
                blankToNull(baseUrl),
                blankToNull(apiKeyRef),
                enabled,
                Objects.requireNonNull(updatedAt, "updatedAt")
        );
    }

    public String getProfileCode() { return profileCode; }
    public String getProviderCode() { return providerCode; }
    public String getModelCode() { return modelCode; }
    public String getBaseUrl() { return baseUrl; }
    public String getApiKeyRef() { return apiKeyRef; }
    public boolean isEnabled() { return enabled; }
    public Instant getUpdatedAt() { return updatedAt; }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " 不能为空");
        }
        return value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
