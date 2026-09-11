package cn.xiaofuge.deepseek.harness.domain.runtime.setting.model.valobj;

import java.time.Instant;

/**
 * 模型设置视图对象。
 * <p>
 * 执行流程：
 * 1. 仓储读出模型设置实体；
 * 2. 领域服务转换成视图对象；
 * 3. API 层直接映射给前端响应。
 *
 * @param profileCode 执行画像编码
 * @param providerCode 提供方编码
 * @param modelCode 模型编码
 * @param baseUrl 提供方基础地址
 * @param apiKeyRef API Key 引用
 * @param protocol 渠道协议编码（openai / anthropic / ollama），空值按 openai 处理
 * @param enabled 是否启用
 * @param active 是否为该 profile 当前生效的模型（多模型时由用户切换）
 * @param updatedAt 更新时间
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record ModelSettingVO(
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
    /**
     * 兼容旧调用方的紧凑构造（active 默认 false）。
     */
    public ModelSettingVO(String profileCode, String providerCode, String modelCode,
                          String baseUrl, String apiKeyRef, String protocol,
                          boolean enabled, Instant updatedAt) {
        this(profileCode, providerCode, modelCode, baseUrl, apiKeyRef, protocol, enabled, false, updatedAt);
    }
}
