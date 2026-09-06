package cn.walioffice.deepseek.harness.domain.sdk.model.valobj;

/**
 * Wire-stable server identity returned by initialization.
 *
 * @param serverName    wire-stable server identity
 * @param serverVersion server version
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record InitializeResult(String serverName, String serverVersion) {
    public static final String SERVER_NAME = "deepseek-harness-sdk-runtime";
}
