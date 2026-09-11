package cn.xiaofuge.deepseek.harness.api.dto;

/**
 * 承载用户编辑后的一条模型设置记录。
 *
 * <p>调用流程：Controller/DTO → 应用门面 → 用例；响应对象由用例结果转换。</p>
 * <p>示例：DTO 只做协议隔离，不承载领域规则。</p>
 *
 * @param protocol 渠道协议编码（openai / anthropic / ollama），空值按 openai 兼容处理
 * @author 小傅哥
 * @website bugstack.cn
 */
public record SaveModelSettingRequestDTO(
        String profileCode,
        String providerCode,
        String modelCode,
        String baseUrl,
        String apiKeyRef,
        String protocol,
        boolean enabled
) {
}
