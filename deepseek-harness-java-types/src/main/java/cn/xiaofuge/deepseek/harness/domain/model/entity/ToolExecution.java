package cn.xiaofuge.deepseek.harness.domain.model.entity;

/**
 * 注册表流水线中的一个待执行工具调用。
 * 对应 TS 侧 {@code ToolExecution}。
 *
 * <p>在 {@link ToolExecutionInput} 基础上补充注册表分配的身份：
 * <ul>
 *   <li>{@link #rootCallId} — 根调用和嵌套调用都会解析该字段</li>
 * </ul>
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record ToolExecution(
        String callId,
        String rootCallId,
        String name,
        Object arguments,
        String token
) {
    public ToolExecution {
        if (callId == null || callId.isBlank()) {
            throw new IllegalArgumentException("callId must be non-blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must be non-blank");
        }
        if (token == null) {
            token = java.util.UUID.randomUUID().toString();
        }
    }

/** 根据调用方输入构造，并分配新的执行标识。 */
    public static ToolExecution from(ToolExecutionInput input) {
        String root = input.rootCallId() != null ? input.rootCallId() : input.callId();
        return new ToolExecution(input.callId(), root, input.name(),
                input.arguments(), java.util.UUID.randomUUID().toString());
    }
}
