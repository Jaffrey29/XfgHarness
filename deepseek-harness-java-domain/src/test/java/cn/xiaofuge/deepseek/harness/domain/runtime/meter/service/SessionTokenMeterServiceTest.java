package cn.xiaofuge.deepseek.harness.domain.runtime.meter.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cn.xiaofuge.deepseek.harness.domain.llm.model.valobj.TokenUsage;
import cn.xiaofuge.deepseek.harness.domain.runtime.meter.model.valobj.TokenUsageSummaryVO;
import cn.xiaofuge.deepseek.harness.domain.session.ISessionEventLogService;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionEvent;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionHeader;
import cn.xiaofuge.deepseek.harness.domain.session.event.service.SessionEventFactory;
import cn.xiaofuge.deepseek.harness.domain.model.entity.Message;
import cn.xiaofuge.deepseek.harness.domain.model.entity.MessageSource;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.TextBlock;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class SessionTokenMeterServiceTest {

    private final String sessionId = "meter-session";
    private final SessionHeader header = SessionHeader.create(sessionId, Instant.EPOCH, null, null);
    private final List<SessionEvent> events = List.of(
            SessionEventFactory.assistantMessage(1, 1,
                    Message.createAssistant(List.of(new TextBlock("one")),
                            new MessageSource.ModelMessageSource("provider", "model")),
                    new TokenUsage(10, 4, 2, 1, 3)),
            SessionEventFactory.assistantMessage(1, 2,
                    Message.createAssistant(List.of(new TextBlock("two")),
                            new MessageSource.ModelMessageSource("provider", "model")),
                    new TokenUsage(5, 6))
    );
    private final SessionTokenMeterService service = new SessionTokenMeterService(new StubEventLogService());

    @Test
    void shouldAggregateModelUsage() {
        TokenUsageSummaryVO summary = service.summarize(sessionId);

        assertEquals(2, summary.modelCalls());
        assertEquals(15, summary.total().inputTokens());
        assertEquals(10, summary.total().outputTokens());
        assertEquals(2, summary.total().cacheReadTokens());
        assertEquals(1, summary.total().cacheWriteTokens());
        assertEquals(3, summary.total().reasoningTokens());
    }

    private final class StubEventLogService implements ISessionEventLogService {
        @Override public SessionHeader createSession(SessionHeader header) { return header; }
        @Override public SessionEvent append(String sessionId, SessionEvent event) { return event; }
        @Override public List<SessionEvent> appendBatch(String sessionId, List<SessionEvent> events) { return events; }
        @Override public List<SessionEvent> readAll(String sessionId) { return events; }
        @Override public List<SessionEvent> readAfter(String sessionId, long afterSeq) { return events; }
        @Override public SessionHeader getHeader(String sessionId) { return header; }
        @Override public long getLastSeq(String sessionId) { return 1; }
        @Override public void restore(String sessionId) {}
    }
}
