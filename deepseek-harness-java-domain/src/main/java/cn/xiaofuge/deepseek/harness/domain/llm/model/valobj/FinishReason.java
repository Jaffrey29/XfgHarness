package cn.xiaofuge.deepseek.harness.domain.llm.model.valobj;

/**
 * Why a model response stopped.
 * <p>
 * Sealed hierarchy mirroring TS {@code FinishReason}. Adapters map
 * provider-specific stop reasons into these variants.
 *
 * <p>调用流程：应用领域规则 → 更新聚合或值对象 → 发布事件/返回结果。</p>
 * <p>示例：领域对象只暴露业务语言方法，不直接依赖基础设施。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public sealed interface FinishReason permits
        FinishReason.Stop,
        FinishReason.ToolCalls,
        FinishReason.MaxTokens,
        FinishReason.Aborted,
        FinishReason.Error {

    /** Model stopped naturally (end-of-text, stop sequence, etc.) */
    record Stop() implements FinishReason {}

    /** Model stopped to request tool calls. */
    record ToolCalls() implements FinishReason {}

    /** Model hit the max output token limit. */
    record MaxTokens() implements FinishReason {}

    /** Request was aborted (caller cancellation, timeout, etc.) */
    record Aborted(LlmFailure failure) implements FinishReason {
        public Aborted {
            java.util.Objects.requireNonNull(failure, "failure");
        }
    }

    /** Provider returned an error. */
    record Error(LlmFailure failure) implements FinishReason {
        public Error {
            java.util.Objects.requireNonNull(failure, "failure");
        }
    }

    /** Convenience: is this a terminal error or abort? */
    default boolean isFailure() {
        return this instanceof Aborted || this instanceof Error;
    }
}
