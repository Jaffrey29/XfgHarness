package cn.walioffice.deepseek.harness.api.service.command;

import static org.junit.jupiter.api.Assertions.*;

import cn.walioffice.deepseek.harness.api.dto.terminal.TerminalOpenRequest;
import cn.walioffice.deepseek.harness.api.dto.terminal.TerminalSendRequest;
import cn.walioffice.deepseek.harness.api.response.terminal.TerminalSendResponse;
import cn.walioffice.deepseek.harness.api.response.terminal.TerminalSessionResponse;
import cn.walioffice.deepseek.harness.domain.terminal.ITerminalCommandService;
import cn.walioffice.deepseek.harness.domain.terminal.model.valobj.TerminalSendResultVO;
import cn.walioffice.deepseek.harness.domain.terminal.model.valobj.TerminalSessionSnapshotVO;
import cn.walioffice.deepseek.harness.domain.terminal.model.valobj.TerminalSessionStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class TerminalCommandApiTest {

    @Test
    void shouldDelegateOpenSendAndClose() {
        StubTerminalCommandService service = new StubTerminalCommandService();
        TerminalCommandApi api = new TerminalCommandApi(service);

        TerminalSessionResponse opened = api.open(new TerminalOpenRequest("演示", "/tmp"));
        assertEquals("term-1", opened.sessionId());
        assertEquals("演示", service.lastOpenName);

        TerminalSendResponse sent = api.send("term-1", new TerminalSendRequest("echo hi", null));
        assertEquals("echo hi", sent.command());
        assertEquals(120_000L, service.lastTimeoutMs);

        TerminalSessionResponse closed = api.close("term-1");
        assertEquals(TerminalSessionStatus.CLOSED.name(), closed.status());
    }

    @Test
    void shouldRejectBlankSessionId() {
        TerminalCommandApi api = new TerminalCommandApi(new StubTerminalCommandService());
        assertThrows(IllegalArgumentException.class, () -> api.send(" ", new TerminalSendRequest("echo hi", 1L)));
        assertThrows(IllegalArgumentException.class, () -> api.close(" "));
    }

    private static final class StubTerminalCommandService implements ITerminalCommandService {
        private String lastOpenName;
        private long lastTimeoutMs;

        @Override
        public TerminalSessionSnapshotVO open(String name, String cwd) {
            lastOpenName = name;
            return new TerminalSessionSnapshotVO("term-1", name, cwd, TerminalSessionStatus.RUNNING, Instant.EPOCH, Instant.EPOCH, null, "", 0);
        }

        @Override
        public cn.walioffice.deepseek.harness.domain.terminal.model.valobj.TerminalSendResultVO send(String sessionId, String command, long timeoutMs) {
            lastTimeoutMs = timeoutMs;
            return new TerminalSendResultVO(sessionId, command, "ok", true, false, TerminalSessionStatus.RUNNING, null, 1L, Instant.EPOCH);
        }

        @Override
        public TerminalSessionSnapshotVO close(String sessionId) {
            return new TerminalSessionSnapshotVO(sessionId, "demo", "/tmp", TerminalSessionStatus.CLOSED, Instant.EPOCH, Instant.EPOCH, null, "", 0);
        }
    }
}
