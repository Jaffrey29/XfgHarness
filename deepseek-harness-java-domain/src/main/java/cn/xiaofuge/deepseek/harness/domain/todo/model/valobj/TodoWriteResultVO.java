package cn.xiaofuge.deepseek.harness.domain.todo.model.valobj;

import java.util.List;

/**
 * 待办写入结果。
 * <p>
 * 执行流程：
 * 1. 领域层保存完整待办列表；
 * 2. 统计状态分布；
 * 3. 返回给工具层输出。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record TodoWriteResultVO(
        List<TodoItem> todos,
        int pending,
        int inProgress,
        int completed
) {
    public TodoWriteResultVO {
        todos = todos == null ? List.of() : List.copyOf(todos);
    }
}
