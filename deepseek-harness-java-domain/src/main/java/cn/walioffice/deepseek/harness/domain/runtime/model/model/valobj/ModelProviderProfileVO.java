package cn.walioffice.deepseek.harness.domain.runtime.model.model.valobj;

/**
 * Represents the model provider resolved for one execution profile.
 *
 * @param providerCode provider identifier
 * @param modelCode model identifier
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record ModelProviderProfileVO(
        String providerCode,
        String modelCode
) {
}
