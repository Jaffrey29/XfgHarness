package cn.xiaofuge.deepseek.harness.domain.runtime.meter.service;

import cn.xiaofuge.deepseek.harness.domain.llm.model.valobj.TokenUsage;
import cn.xiaofuge.deepseek.harness.domain.runtime.meter.ITokenMeterService;
import cn.xiaofuge.deepseek.harness.domain.runtime.meter.model.valobj.TokenUsageSummaryVO;
import cn.xiaofuge.deepseek.harness.domain.session.ISessionEventLogService;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionEvent;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class SessionTokenMeterService implements ITokenMeterService {

    private final ISessionEventLogService eventLogService;

    public SessionTokenMeterService(ISessionEventLogService eventLogService) {
        this.eventLogService = Objects.requireNonNull(eventLogService, "eventLogService");
    }

    @Override
    public TokenUsageSummaryVO summarize(String sessionId) {
        TokenUsage total = new TokenUsage(0, 0);
        long modelCalls = 0;
        for (SessionEvent event : eventLogService.readAll(sessionId)) {
            if (event instanceof SessionEvent.AssistantMessage message && message.data().usage() != null) {
                total = total.plus(message.data().usage());
                modelCalls++;
            }
        }
        return new TokenUsageSummaryVO(sessionId, modelCalls, total);
    }
}
