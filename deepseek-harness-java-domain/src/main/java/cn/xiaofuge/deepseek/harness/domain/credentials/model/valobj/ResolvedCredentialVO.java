package cn.xiaofuge.deepseek.harness.domain.credentials.model.valobj;

/**
 * One resolved credential value and the source layer that supplied it.
 *
 * @param value  the non-empty secret value
 * @param source provider-defined source layer id (e.g. {@code env}, {@code file}, {@code project-env}, {@code user-env})
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record ResolvedCredentialVO(String value, String source) {
}
