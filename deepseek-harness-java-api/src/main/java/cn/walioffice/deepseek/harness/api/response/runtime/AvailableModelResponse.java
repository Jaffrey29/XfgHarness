package cn.walioffice.deepseek.harness.api.response.runtime;

/**
 * 返回当前 LLM 运行时可用的一个模型。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record AvailableModelResponse(
        String providerCode,
        String modelCode,
        String displayName
) {
}
