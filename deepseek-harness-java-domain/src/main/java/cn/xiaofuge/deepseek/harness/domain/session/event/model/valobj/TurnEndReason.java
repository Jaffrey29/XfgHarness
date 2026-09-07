package cn.xiaofuge.deepseek.harness.domain.session.event.model.valobj;
/**
 * TurnEndReason 定义当前模块中的interface契约与职责。
 */
public sealed interface TurnEndReason permits
        TurnEndReason.Completed,
        TurnEndReason.Aborted,
        TurnEndReason.Blocked,
        TurnEndReason.Error,
        TurnEndReason.MaxTokens,
        TurnEndReason.Interrupted {
    /**
     * Completed 定义当前模块中的record契约与职责。
     */
    record Completed() implements TurnEndReason {}
    /**
     * Aborted 定义当前模块中的record契约与职责。
     */
    record Aborted(String reason) implements TurnEndReason {
        public Aborted { reason = reason == null ? "" : reason; }
    }
    /**
     * Blocked 定义当前模块中的record契约与职责。
     */
    record Blocked() implements TurnEndReason {}
    /**
     * Error 定义当前模块中的record契约与职责。
     */
    record Error(String message, String code) implements TurnEndReason {
        public Error { message = message == null ? "" : message; code = code == null ? "UNKNOWN" : code; }
    }
    /**
     * MaxTokens 定义当前模块中的record契约与职责。
     */
    record MaxTokens() implements TurnEndReason {}
    /**
     * Interrupted 定义当前模块中的record契约与职责。
     */
    record Interrupted() implements TurnEndReason {}
}
