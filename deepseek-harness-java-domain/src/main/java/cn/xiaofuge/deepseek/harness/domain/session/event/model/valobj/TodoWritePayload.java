package cn.xiaofuge.deepseek.harness.domain.session.event.model.valobj;
import java.util.List;
/**
 * TodoWritePayload 定义当前模块中的record契约与职责。
 */
public record TodoWritePayload(List<TodoItemPayload> todos) {
    public TodoWritePayload {
        todos = List.copyOf(todos);
    }
}
