package cn.xiaofuge.deepseek.harness.infrastructure.gateway.dto;

/**
 * Returns the provider/model tuple for a profile.
 *
 * @param providerCode provider identifier
 * @param modelCode model identifier
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record ModelResponseDTO(String providerCode, String modelCode) {
}
