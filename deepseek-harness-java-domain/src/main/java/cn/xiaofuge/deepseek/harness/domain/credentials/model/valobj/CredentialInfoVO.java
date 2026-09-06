package cn.xiaofuge.deepseek.harness.domain.credentials.model.valobj;

import java.util.Optional;

/**
 * Source and writability facts for one reference, safe for configuration UIs — never the value.
 *
 * @param configured whether {@code resolve} would currently return a value
 * @param source     source layer currently supplying the value; absent while unconfigured
 * @param writable   whether {@code set} would currently succeed for this reference
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record CredentialInfoVO(boolean configured, Optional<String> source, boolean writable) {
}
