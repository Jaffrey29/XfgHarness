package cn.xiaofuge.deepseek.harness.domain.tool.service;

/**
 * Converts a model-provided JSON argument string into the tool execution value.
 * <p>调用流程：Agent 发起 tool call → ToolRegistry 查找工具 → Guard 校验 → ToolExecutor 执行 → 结果回填。</p>
 * <p>示例：工具名 fs_read 与 JSON 参数先经过校验，再生成 ToolExecutionResult。</p>
 */
@FunctionalInterface
public interface ToolArgumentsParser {

    Object parse(String arguments);

    static ToolArgumentsParser identity() {
        return arguments -> arguments;
    }
}
