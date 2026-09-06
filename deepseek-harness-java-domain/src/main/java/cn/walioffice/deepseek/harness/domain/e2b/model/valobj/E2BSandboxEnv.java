package cn.walioffice.deepseek.harness.domain.e2b.model.valobj;

/**
 * E2B sandbox environment configuration.
 *
 * @param sandboxId    unique sandbox identifier (assigned by provider)
 * @param apiKey       API key for the provider
 * @param template     sandbox template identifier
 * @param timeoutMs    sandbox lifetime in milliseconds
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record E2BSandboxEnv(String sandboxId, String apiKey, String template, long timeoutMs) {
    public static E2BSandboxEnv of(String template, long timeoutMs) {
        return new E2BSandboxEnv(null, null, template, timeoutMs);
    }
}
