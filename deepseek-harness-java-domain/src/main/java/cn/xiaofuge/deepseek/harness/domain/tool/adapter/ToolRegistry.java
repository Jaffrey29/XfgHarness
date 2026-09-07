package cn.xiaofuge.deepseek.harness.domain.tool.adapter;

import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolDefinition;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolExecutionInput;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.ToolExecutionMode;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.ToolSchema;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Registry and lookup for tool definitions.
 * Mirrors TS {@code ToolRuntime} (registry portion).
 *
 * <p>调用流程：Agent 发起 tool call → ToolRegistry 查找工具 → Guard 校验 → ToolExecutor 执行 → 结果回填。</p>
 * <p>示例：工具名 fs_read 与 JSON 参数先经过校验，再生成 ToolExecutionResult。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface ToolRegistry {

    /** Register a tool definition. Returns a disposer. */
    Runnable register(ToolDefinition definition);

    /** Look up a tool by name. */
    Optional<ToolDefinition> lookup(String name);

    /** All registered tool definitions. */
    Collection<ToolDefinition> all();

    /** Determine the execution mode for a pending call. */
    ToolExecutionMode executionMode(ToolExecutionInput exec);

    /** All registered tool schemas (for LLM request building). */
    default List<ToolSchema> schemas() {
        return all().stream()
                .map(td -> new ToolSchema(td.name(), td.description(), td.parameters()))
                .toList();
    }
}
