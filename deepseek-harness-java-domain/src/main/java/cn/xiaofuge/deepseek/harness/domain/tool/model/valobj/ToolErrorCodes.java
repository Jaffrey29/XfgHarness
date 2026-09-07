package cn.xiaofuge.deepseek.harness.domain.tool.model.valobj;

/**
 * Canonical error codes for tool call cancellation.
 * Mirrors TS {@code TOOL_ABORTED} / {@code TOOL_ABORTED_BEFORE_DISPATCH}.
 *
 * <p>调用流程：Agent 发起 tool call → ToolRegistry 查找工具 → Guard 校验 → ToolExecutor 执行 → 结果回填。</p>
 * <p>示例：工具名 fs_read 与 JSON 参数先经过校验，再生成 ToolExecutionResult。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public final class ToolErrorCodes {
    public static final String ABORTED = "ABORTED";
    public static final String ABORTED_BEFORE_DISPATCH = "ABORTED_BEFORE_DISPATCH";
    public static final String UNKNOWN_TOOL = "UNKNOWN_TOOL";
    public static final String INVALID_ARGS = "INVALID_ARGS";
    public static final String INVALID_TOOL_OUTPUT = "INVALID_TOOL_OUTPUT";

    /**
     * 转换为 ol Error Codes。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @return 方法执行结果。
     */
    private ToolErrorCodes() {}
}
