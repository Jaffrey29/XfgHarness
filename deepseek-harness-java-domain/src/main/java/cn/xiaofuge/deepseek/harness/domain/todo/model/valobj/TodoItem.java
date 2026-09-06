package cn.xiaofuge.deepseek.harness.domain.todo.model.valobj;

/**
 * 待办项。
 * <p>
 * 执行流程：
 * 1. 工具接收完整任务列表；
 * 2. 领域层校验内容和状态；
 * 3. 写入会话事件并返回最新快照。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record TodoItem(
        String content,
        Status status
) {
    /** 待办状态。 */
    public enum Status {
        PENDING("pending"),
        IN_PROGRESS("in_progress"),
        COMPLETED("completed");

        private final String wire;

        Status(String wire) {
            this.wire = wire;
        }

        public String wire() {
            return wire;
        }

        public static Status fromWire(String value) {
            return switch (value) {
                case "pending" -> PENDING;
                case "in_progress" -> IN_PROGRESS;
                case "completed" -> COMPLETED;
                default -> throw new IllegalArgumentException("unknown todo status: " + value);
            };
        }
    }

    public TodoItem {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("todo content must be non-empty");
        }
        if (status == null) {
            throw new IllegalArgumentException("todo status must be non-null");
        }
    }
}
