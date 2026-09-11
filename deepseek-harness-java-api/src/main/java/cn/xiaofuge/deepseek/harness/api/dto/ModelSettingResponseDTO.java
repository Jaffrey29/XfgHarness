package cn.xiaofuge.deepseek.harness.api.dto;

import java.time.Instant;

/**
 * 返回一条用户可见的模型设置记录。
 *
 * <p>调用流程：Controller/DTO → 应用门面 → 用例；响应对象由用例结果转换。</p>
 * <p>示例：DTO 只做协议隔离，不承载领域规则。</p>
 *
 * @param protocol 渠道协议编码（openai / anthropic / ollama）
 * @param active 是否为该 profile 当前生效的模型
 * @author 小傅哥
 * @website bugstack.cn
 */
public record ModelSettingResponseDTO(
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
    public ModelSettingResponseDTO(String profileCode, String providerCode, String modelCode,
                                   String baseUrl, String apiKeyRef, String protocol,
                                   boolean enabled, Instant updatedAt) {
        this(profileCode, providerCode, modelCode, baseUrl, apiKeyRef, protocol, enabled, false, updatedAt);
    }
}
