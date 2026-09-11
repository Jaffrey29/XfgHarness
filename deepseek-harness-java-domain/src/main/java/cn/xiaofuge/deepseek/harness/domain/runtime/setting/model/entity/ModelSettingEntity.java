package cn.xiaofuge.deepseek.harness.domain.runtime.setting.model.entity;

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
 * <p>调用流程：应用领域规则 → 更新聚合或值对象 → 发布事件/返回结果。</p>
 * <p>示例：领域对象只暴露业务语言方法，不直接依赖基础设施。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public class ModelSettingEntity {

    private final String profileCode;
    private final String providerCode;
    private final String modelCode;
    private final String baseUrl;
    private final String apiKeyRef;
    /** 渠道协议编码：openai / anthropic / ollama；空值按 openai 兼容处理。 */
    private final String protocol;
    private final boolean enabled;
    /** 是否为该 profile 当前生效的模型（同 profile 多模型时由用户切换）。 */
    private final boolean active;
    private final Instant updatedAt;

    /**
     * 创建模型设置实体。
     * <p>用途：承载模型提供方、模型编码、协议类型、基础地址和启用状态。</p>
     */
    public ModelSettingEntity(
            String profileCode,
            String providerCode,
            String modelCode,
            String baseUrl,
            String apiKeyRef,
            String protocol,
            boolean enabled,
            Instant updatedAt
    ) {
        this(profileCode, providerCode, modelCode, baseUrl, apiKeyRef, protocol, enabled, false, updatedAt);
    }

    /**
     * 创建模型设置实体（含激活标记）。
     */
    public ModelSettingEntity(
            String profileCode,
            String providerCode,
            String modelCode,
            String baseUrl,
            String apiKeyRef,
            String protocol,
            boolean enabled,
            boolean active,
            Instant updatedAt
    ) {
        this.profileCode = Objects.requireNonNull(profileCode, "profileCode");
        this.providerCode = Objects.requireNonNull(providerCode, "providerCode");
        this.modelCode = Objects.requireNonNull(modelCode, "modelCode");
        this.baseUrl = baseUrl;
        this.apiKeyRef = apiKeyRef;
        this.protocol = blankToNull(protocol) == null ? "openai" : blankToNull(protocol);
        this.enabled = enabled;
        this.active = active;
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
            String protocol,
            boolean enabled
    ) {
        return create(profileCode, providerCode, modelCode, baseUrl, apiKeyRef, protocol, enabled, false);
    }

    /**
     * 创建一个新的模型设置实体（含激活标记）。
     */
    public static ModelSettingEntity create(
            String profileCode,
            String providerCode,
            String modelCode,
            String baseUrl,
            String apiKeyRef,
            String protocol,
            boolean enabled,
            boolean active
    ) {
        return new ModelSettingEntity(
                requireText(profileCode, "profileCode"),
                requireText(providerCode, "providerCode"),
                requireText(modelCode, "modelCode"),
                blankToNull(baseUrl),
                blankToNull(apiKeyRef),
                blankToNull(protocol),
                enabled,
                active,
                Instant.now()
        );
    }

    /**
     * 重新配置当前模型设置。
     * <p>
     * 执行流程：
     * 1. 保留原有 profileCode；
     * 2. 更新提供方、模型、协议和访问参数；
     * 3. 使用新的更新时间生成新实体。
     */
    public ModelSettingEntity reconfigure(
            String providerCode,
            String modelCode,
            String baseUrl,
            String apiKeyRef,
            String protocol,
            boolean enabled,
            Instant updatedAt
    ) {
        return new ModelSettingEntity(
                profileCode,
                requireText(providerCode, "providerCode"),
                requireText(modelCode, "modelCode"),
                blankToNull(baseUrl),
                blankToNull(apiKeyRef),
                blankToNull(protocol),
                enabled,
                Objects.requireNonNull(updatedAt, "updatedAt")
        );
    }

    /**
     * 获取画像ProfileCode。
     * @return 领域处理结果。
     */
    public String getProfileCode() { return profileCode; }
    /**
     * 获取Provider Code。
     */
    public String getProviderCode() { return providerCode; }
    /**
     * 获取Model Code。
     */
    public String getModelCode() { return modelCode; }
    /**
     * 获取Base Url。
     */
    public String getBaseUrl() { return baseUrl; }
    /**
     * 获取Api Key Ref。
     */
    public String getApiKeyRef() { return apiKeyRef; }
    /**
     * 获取渠道协议编码；可能为空，空值按 openai 兼容协议处理。
     */
    public String getProtocol() { return protocol; }
    /**
     * 判断Enabled是否成立。
     */
    public boolean isEnabled() { return enabled; }
    /**
     * 判断是否为该 profile 当前生效模型。
     */
    public boolean isActive() { return active; }
    /**
     * 获取Updated At。
     */
    public Instant getUpdatedAt() { return updatedAt; }

    /**
     * 校验必需条件；不满足时抛出异常。
     */
    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " 不能为空");
        }
        return value;
    }

    /**
     * 处理blank To Null。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param value 参数值。
     * @return 方法执行结果。
     */
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
