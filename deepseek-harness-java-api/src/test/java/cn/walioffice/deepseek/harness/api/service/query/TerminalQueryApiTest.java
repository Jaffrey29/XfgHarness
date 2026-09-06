package cn.walioffice.deepseek.harness.api.service.query;

import static org.junit.jupiter.api.Assertions.*;

import cn.walioffice.deepseek.harness.api.response.terminal.TerminalReadResponse;
import cn.walioffice.deepseek.harness.api.response.terminal.TerminalSessionResponse;
import cn.walioffice.deepseek.harness.domain.terminal.ITerminalQueryService;
import cn.walioffice.deepseek.harness.domain.terminal.model.valobj.TerminalReadResultVO;
import cn.walioffice.deepseek.harness.domain.terminal.model.valobj.TerminalSessionSnapshotVO;
import cn.walioffice.deepseek.harness.domain.terminal.model.valobj.TerminalSessionStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class TerminalQueryApiTest {

    @Test
    void shouldMapListAndReadResponses() {
        StubTerminalQueryService service = new StubTerminalQueryService();
        TerminalQueryApi api = new TerminalQueryApi(service);

        List<TerminalSessionResponse> sessions = api.listSessions();
        assertEquals(1, sessions.size());
        assertEquals("term-1", sessions.get(0).sessionId());

        TerminalReadResponse read = api.read("term-1", 100);
        assertEquals("output", read.content());
        assertEquals("term-1", service.lastReadSessionId);
    }

    @Test
    void shouldRejectBlankSessionId() {
        TerminalQueryApi api = new TerminalQueryApi(new StubTerminalQueryService());
        assertThrows(IllegalArgumentException.class, () -> api.read("", 10));
    }

    private static final class StubTerminalQueryService implements ITerminalQueryService {
        private String lastReadSessionId;

        @Override
        public List<TerminalSessionSnapshotVO> list() {
            return List.of(new TerminalSessionSnapshotVO("term-1", "demo", "/tmp", TerminalSessionStatus.RUNNING, Instant.EPOCH, Instant.EPOCH, null, "", 0));
        }

        @Override
        public TerminalReadResultVO read(String sessionId, int maxChars) {
            lastReadSessionId = sessionId;
            return new TerminalReadResultVO(sessionId, "output", false, TerminalSessionStatus.RUNNING, null);
        }
    }
}
