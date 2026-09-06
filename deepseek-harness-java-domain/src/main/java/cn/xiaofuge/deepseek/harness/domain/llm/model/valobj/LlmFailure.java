package cn.xiaofuge.deepseek.harness.domain.llm.model.valobj;

/**
 * Serializable provider or transport failure facts.
 * <p>
 * Policy decides whether a failure is retryable; this record is purely
 * informational. Mirrors TS {@code LlmFailure}.
 *
 * @param message             human-readable failure message
 * @param code                stable provider-neutral machine-routing code
 * @param status              HTTP status (nullable)
 * @param providerRetryAfterMs provider-requested delay in ms (nullable)
 * @param requestId           provider-issued request id for diagnostics (nullable)
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record LlmFailure(
        String message,
        String code,
        Integer status,
        Long providerRetryAfterMs,
        String requestId
) {
    public LlmFailure {
        java.util.Objects.requireNonNull(message, "message");
        java.util.Objects.requireNonNull(code, "code");
    }

    /** Convenience constructor for message + code only. */
    public LlmFailure(String message, String code) {
        this(message, code, null, null, null);
    }
}
