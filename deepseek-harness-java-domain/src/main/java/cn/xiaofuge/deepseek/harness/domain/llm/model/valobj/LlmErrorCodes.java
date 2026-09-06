package cn.xiaofuge.deepseek.harness.domain.llm.model.valobj;

/**
 * Harness-owned LLM error codes.
 * <p>
 * Stable codes used across retry policies, adapter failures, and diagnostics.
 * Mirrors TS {@code LlmError} code constants.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public final class LlmErrorCodes {
    private LlmErrorCodes() {}

    public static final String EMPTY_RESPONSE = "EMPTY_RESPONSE";
    public static final String QUOTA_EXCEEDED = "QUOTA_EXCEEDED";
    public static final String CONTEXT_WINDOW_EXCEEDED = "CONTEXT_WINDOW_EXCEEDED";
    public static final String RATE_LIMIT = "RATE_LIMIT";
    public static final String SERVER = "SERVER";
    public static final String TIMEOUT = "TIMEOUT";
    public static final String TRANSPORT = "TRANSPORT";
    public static final String AUTH = "AUTH";
    public static final String INVALID_REQUEST = "INVALID_REQUEST";
    public static final String MALFORMED_RESPONSE = "MALFORMED_RESPONSE";
    public static final String STREAM_CLOSED = "STREAM_CLOSED";
    public static final String ABORTED = "ABORTED";
    public static final String UNKNOWN = "UNKNOWN";

    /** Default retryable codes for the normal retry policy. */
    public static final java.util.List<String> DEFAULT_RETRYABLE_CODES = java.util.List.of(
            EMPTY_RESPONSE, RATE_LIMIT, SERVER, TIMEOUT, TRANSPORT
    );
}
