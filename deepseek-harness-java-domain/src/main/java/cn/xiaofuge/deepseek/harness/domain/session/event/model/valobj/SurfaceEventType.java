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

    /**
     * 判断Surface Event Type是否成立。
     */
    public static boolean isSurfaceEventType(SessionEventType type) {
        return switch (type) {
            case USER_MESSAGE, ASSISTANT_MESSAGE, TOOL_RESULT -> true;
            default -> false;
        };
    }

    /**
     * 转换为 会话 事件 类型。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @return 转换后的目标对象。
     */
    public SessionEventType toSessionEventType() {
        return switch (this) {
            case USER_MESSAGE -> SessionEventType.USER_MESSAGE;
            case ASSISTANT_MESSAGE -> SessionEventType.ASSISTANT_MESSAGE;
            case TOOL_RESULT -> SessionEventType.TOOL_RESULT;
        };
    }
}
