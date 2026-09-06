package cn.walioffice.deepseek.harness.domain.guard.model.valobj;

/**
 * Result substituted when a tool-call timeout policy wins.
 *
 * @param timeoutMs the elapsed budget
 * @param message   model-facing error message
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record ToolTimeoutResult(long timeoutMs, String message) {

    public static final String CODE = "TOOL_TIMEOUT";

    public ToolTimeoutResult(long timeoutMs) {
        this(timeoutMs, "tool call timed out after " + timeoutMs + "ms");
    }
}
