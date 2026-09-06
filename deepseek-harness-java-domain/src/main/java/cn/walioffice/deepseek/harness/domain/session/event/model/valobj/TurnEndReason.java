package cn.walioffice.deepseek.harness.domain.session.event.model.valobj;
public sealed interface TurnEndReason permits
        TurnEndReason.Completed,
        TurnEndReason.Aborted,
        TurnEndReason.Blocked,
        TurnEndReason.Error,
        TurnEndReason.MaxTokens,
        TurnEndReason.Interrupted {
    record Completed() implements TurnEndReason {}
    record Aborted(String reason) implements TurnEndReason {
        public Aborted { reason = reason == null ? "" : reason; }
    }
    record Blocked() implements TurnEndReason {}
    record Error(String message, String code) implements TurnEndReason {
        public Error { message = message == null ? "" : message; code = code == null ? "UNKNOWN" : code; }
    }
    record MaxTokens() implements TurnEndReason {}
    record Interrupted() implements TurnEndReason {}
}
