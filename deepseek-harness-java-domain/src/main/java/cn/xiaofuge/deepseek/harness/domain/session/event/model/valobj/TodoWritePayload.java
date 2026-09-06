package cn.xiaofuge.deepseek.harness.domain.session.event.model.valobj;
import java.util.List;
public record TodoWritePayload(List<TodoItemPayload> todos) {
    public TodoWritePayload {
        todos = List.copyOf(todos);
    }
}
