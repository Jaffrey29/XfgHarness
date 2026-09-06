package cn.xiaofuge.deepseek.harness.domain.session.event.model.valobj;

/**
 * Subset of {@link SessionEventType} whose events may carry
 * {@link SurfaceOp} and {@code sourceEventSeqs} — i.e. events that
 * contribute to the visible "surface" of a conversation.
 * <p>
 * Only user messages, assistant messages, and tool results are surface events.
 * All other event types (boundaries, chunks, headers, etc.) are never surface events.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public enum SurfaceEventType {
    USER_MESSAGE,
    ASSISTANT_MESSAGE,
    TOOL_RESULT;

    public static boolean isSurfaceEventType(SessionEventType type) {
        return switch (type) {
            case USER_MESSAGE, ASSISTANT_MESSAGE, TOOL_RESULT -> true;
            default -> false;
        };
    }

    public SessionEventType toSessionEventType() {
        return switch (this) {
            case USER_MESSAGE -> SessionEventType.USER_MESSAGE;
            case ASSISTANT_MESSAGE -> SessionEventType.ASSISTANT_MESSAGE;
            case TOOL_RESULT -> SessionEventType.TOOL_RESULT;
        };
    }
}
