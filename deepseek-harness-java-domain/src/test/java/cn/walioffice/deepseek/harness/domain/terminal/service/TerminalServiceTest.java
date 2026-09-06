package cn.walioffice.deepseek.harness.domain.terminal.service;

import static org.junit.jupiter.api.Assertions.*;

import cn.walioffice.deepseek.harness.domain.terminal.adapter.port.ITerminalPort;
import cn.walioffice.deepseek.harness.domain.terminal.model.valobj.TerminalReadResultVO;
import cn.walioffice.deepseek.harness.domain.terminal.model.valobj.TerminalSendResultVO;
import cn.walioffice.deepseek.harness.domain.terminal.model.valobj.TerminalSessionSnapshotVO;
import cn.walioffice.deepseek.harness.domain.terminal.model.valobj.TerminalSessionStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class TerminalServiceTest {

    @Test
    void shouldDelegateToTerminalPort() {
        StubTerminalPort port = new StubTerminalPort();
        TerminalService service = new TerminalService(port);

        TerminalSessionSnapshotVO opened = service.open("  演示终端  ", "/tmp");
        assertEquals("demo-1", opened.sessionId());
        assertEquals("  演示终端  ", port.lastOpenName);

        TerminalSendResultVO sent = service.send("demo-1", "echo hi", 2000);
        assertEquals("echo hi", sent.command());
        assertEquals(1, port.sendCount);

        TerminalReadResultVO read = service.read("demo-1", 1000);
        assertEquals("output", read.content());
        assertEquals(1, port.readCount);

        TerminalSessionSnapshotVO closed = service.close("demo-1");
        assertEquals(TerminalSessionStatus.CLOSED, closed.status());
        assertEquals(1, port.closeCount);

        assertEquals(1, service.list().size());
    }

    @Test
    void shouldValidateInputs() {
        TerminalService service = new TerminalService(new StubTerminalPort());

        assertThrows(IllegalArgumentException.class, () -> service.send("", "echo hi", 1));
        assertThrows(IllegalArgumentException.class, () -> service.send("demo-1", " ", 1));
        assertThrows(IllegalArgumentException.class, () -> service.read(null, 100));
    }

    private static final class StubTerminalPort implements ITerminalPort {
        private int sendCount;
        private int readCount;
        private int closeCount;
        private String lastOpenName;
        private final TerminalSessionSnapshotVO snapshot = new TerminalSessionSnapshotVO(
                "demo-1",
                "terminal",
                "/tmp",
                TerminalSessionStatus.RUNNING,
                Instant.EPOCH,
                Instant.EPOCH,
                null,
                "",
                0
        );

        @Override
        public TerminalSessionSnapshotVO open(String name, String cwd) {
            lastOpenName = name;
            return snapshot;
        }

        @Override
        public TerminalSendResultVO send(String sessionId, String command, long timeoutMs) {
            sendCount++;
            return new TerminalSendResultVO(sessionId, command, "output", true, false, TerminalSessionStatus.RUNNING, null, 5L, Instant.EPOCH);
        }

        @Override
        public TerminalReadResultVO read(String sessionId, int maxChars) {
            readCount++;
            return new TerminalReadResultVO(sessionId, "output", false, TerminalSessionStatus.RUNNING, null);
        }

        @Override
        public TerminalSessionSnapshotVO close(String sessionId) {
            closeCount++;
            return new TerminalSessionSnapshotVO(
                    sessionId,
                    "terminal",
                    "/tmp",
                    TerminalSessionStatus.CLOSED,
                    Instant.EPOCH,
                    Instant.EPOCH,
                    null,
                    "",
                    0
            );
        }

        @Override
        public List<TerminalSessionSnapshotVO> list() {
            return List.of(snapshot);
        }
    }
}
