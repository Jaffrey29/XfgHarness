package cn.xiaofuge.deepseek.harness.api.dto;

import java.time.Instant;

/**
 * 返回一条用户可见的模型设置记录。
 *
 * <p>调用流程：Controller/DTO → 应用门面 → 用例；响应对象由用例结果转换。</p>
 * <p>示例：DTO 只做协议隔离，不承载领域规则。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public record ModelSettingResponseDTO(
        String profileCode,
        String providerCode,
        String modelCode,
        String baseUrl,
        String apiKeyRef,
        boolean enabled,
        Instant updatedAt
) {
}
