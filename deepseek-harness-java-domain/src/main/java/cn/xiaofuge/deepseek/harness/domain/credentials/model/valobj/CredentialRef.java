package cn.xiaofuge.deepseek.harness.domain.credentials.model.valobj;

import java.util.regex.Pattern;

/**
 * Nominal reference to one credential: a POSIX-style environment-variable name.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record CredentialRef(String value) {

    private static final Pattern REF_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");

    public CredentialRef {
        if (value == null || !REF_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "credential ref \"" + value + "\" must match " + REF_PATTERN);
        }
    }

    /**
     * 处理of相关逻辑。
     */
    public static CredentialRef of(String value) {
        return new CredentialRef(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
