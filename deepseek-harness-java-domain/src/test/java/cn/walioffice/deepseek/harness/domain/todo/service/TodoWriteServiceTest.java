package cn.walioffice.deepseek.harness.domain.todo.service;

import static org.junit.jupiter.api.Assertions.*;

import cn.walioffice.deepseek.harness.domain.session.event.model.entity.SessionHeader;
import cn.walioffice.deepseek.harness.domain.session.event.model.entity.SessionLog;
import cn.walioffice.deepseek.harness.domain.todo.model.valobj.TodoItem;
import cn.walioffice.deepseek.harness.domain.todo.model.valobj.TodoItem.Status;
import cn.walioffice.deepseek.harness.domain.todo.model.valobj.TodoWriteResultVO;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class TodoWriteServiceTest {

    @Test
    void writeShouldNormalizeAndCountTodos() {
        TodoWriteService service = new TodoWriteService();
        SessionLog session = new SessionLog("session-1", SessionHeader.create("session-1", Instant.now(), "/tmp", "agent"));

        TodoWriteResultVO result = service.write(session, List.of(
                new TodoItem("  梳理架构  ", Status.PENDING),
                new TodoItem("补充测试", Status.IN_PROGRESS),
                new TodoItem("提交变更", Status.COMPLETED)
        ), false);

        assertEquals(3, result.todos().size());
        assertEquals("梳理架构", result.todos().get(0).content());
        assertEquals(1, result.pending());
        assertEquals(1, result.inProgress());
        assertEquals(1, result.completed());
        assertEquals(1, session.size());
        assertEquals(3, service.foldTodos(session).size());
    }

    @Test
    void writeShouldRejectInvalidTodoShape() {
        TodoWriteService service = new TodoWriteService();
        SessionLog session = new SessionLog("session-2", SessionHeader.create("session-2", Instant.now(), "/tmp", "agent"));

        assertThrows(IllegalArgumentException.class, () -> service.write(session,
                List.of(new TodoItem("任务一", Status.IN_PROGRESS), new TodoItem("任务二", Status.IN_PROGRESS)), false));
        assertThrows(IllegalArgumentException.class, () -> service.write(session,
                List.of(new TodoItem("任务一", Status.PENDING), new TodoItem("任务一", Status.COMPLETED)), true));
        assertThrows(IllegalArgumentException.class, () -> service.write(null,
                List.of(new TodoItem("任务一", Status.PENDING)), true));
        assertThrows(IllegalArgumentException.class, () -> service.foldTodos(null));
    }
}
