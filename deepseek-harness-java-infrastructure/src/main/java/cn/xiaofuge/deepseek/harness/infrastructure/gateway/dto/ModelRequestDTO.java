package cn.xiaofuge.deepseek.harness.infrastructure.gateway.dto;

/**
 * Carries one model-resolution request to the gateway layer.
 *
 * @param profileCode execution profile code
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record ModelRequestDTO(String profileCode) {
}
