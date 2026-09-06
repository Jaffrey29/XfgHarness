package cn.xiaofuge.deepseek.harness.domain.tool.model.valobj;

/**
 * Canonical error codes for tool call cancellation.
 * Mirrors TS {@code TOOL_ABORTED} / {@code TOOL_ABORTED_BEFORE_DISPATCH}.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public final class ToolErrorCodes {
    public static final String ABORTED = "ABORTED";
    public static final String ABORTED_BEFORE_DISPATCH = "ABORTED_BEFORE_DISPATCH";
    public static final String UNKNOWN_TOOL = "UNKNOWN_TOOL";
    public static final String INVALID_ARGS = "INVALID_ARGS";
    public static final String INVALID_TOOL_OUTPUT = "INVALID_TOOL_OUTPUT";

    private ToolErrorCodes() {}
}
