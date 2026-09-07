package cn.xiaofuge.deepseek.harness.cases.support;

/**
 * Common case-level guards.
 */
public final class CaseGuard {

    private CaseGuard() {
    }

    public static String requireSessionId(String sessionId) {
        return requireText(sessionId, "sessionId must not be blank");
    }

    public static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    public static <T> T requireNonNull(T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
