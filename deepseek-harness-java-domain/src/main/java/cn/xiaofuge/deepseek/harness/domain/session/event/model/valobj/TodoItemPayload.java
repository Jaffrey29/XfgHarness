package cn.xiaofuge.deepseek.harness.domain.session.event.model.valobj;
/**
 * TodoItemPayload 定义当前模块中的record契约与职责。
 */
public record TodoItemPayload(String content, String status) {
    public TodoItemPayload {
        if (status == null || (!status.equals("pending") && !status.equals("in_progress") && !status.equals("completed"))) {
            throw new IllegalArgumentException("Invalid todo status: " + status);
        }
    }
}
