package cn.xiaofuge.deepseek.harness.cases.session.event;

import cn.xiaofuge.deepseek.harness.api.dto.session.SessionEventQueryRequest;
import cn.xiaofuge.deepseek.harness.api.dto.session.SessionEventQueryResponse;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionEvent;
import cn.xiaofuge.deepseek.harness.domain.session.event.service.ISessionEventLogService;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 薄委托到 {@link ISessionEventLogService}。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class SessionEventQueryCaseImpl implements ISessionEventQueryCase {

    private final ISessionEventLogService sessionEventLogService;

    public SessionEventQueryCaseImpl(ISessionEventLogService sessionEventLogService) {
        this.sessionEventLogService = sessionEventLogService;
    }

    @Override
    public List<SessionEventQueryResponse> queryEvents(SessionEventQueryRequest request) {
        List<SessionEvent> events;
        if (request.fromSeq() != null) {
            events = sessionEventLogService.readAfter(request.sessionId(), request.fromSeq());
        } else {
            events = sessionEventLogService.readAll(request.sessionId());
        }

        List<SessionEventQueryResponse> result = new ArrayList<>();
        for (SessionEvent e : events) {
            if (request.toSeq() != null && e.seq() > request.toSeq()) break;
            String eventType = e.getClass().getSimpleName();
            if (request.eventTypes() != null && !request.eventTypes().isEmpty()
                    && !request.eventTypes().contains(eventType)) {
                continue;
            }
            result.add(new SessionEventQueryResponse(
                    e.seq(),
                    eventType,
                    e.time(),
                    toMap(e)
            ));
        }
        return result;
    }

    private java.util.Map<String, Object> toMap(SessionEvent e) {
        java.util.Map<String, Object> m = new LinkedHashMap<>();
        m.put("seq", e.seq());
        m.put("type", e.getClass().getSimpleName());
        m.put("time", e.time() != null ? e.time().toString() : null);
        return m;
    }
}
