package cn.xiaofuge.deepseek.harness.cases.session.event;

import cn.xiaofuge.deepseek.harness.cases.session.ISessionEventQueryCase;
import cn.xiaofuge.deepseek.harness.cases.session.SessionEventQuery;
import cn.xiaofuge.deepseek.harness.cases.session.SessionEventView;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionEvent;
import cn.xiaofuge.deepseek.harness.domain.session.ISessionEventLogService;
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

    /**
     * 处理Session Event Query Case Impl相关逻辑。
     */
    public SessionEventQueryCaseImpl(ISessionEventLogService sessionEventLogService) {
        this.sessionEventLogService = sessionEventLogService;
    }

    @Override
    public List<SessionEventView> queryEvents(SessionEventQuery query) {
        List<SessionEvent> events;
        if (query.fromSeq() != null) {
            events = sessionEventLogService.readAfter(query.sessionId(), query.fromSeq());
        } else {
            events = sessionEventLogService.readAll(query.sessionId());
        }

        List<SessionEventView> result = new ArrayList<>();
        for (SessionEvent e : events) {
            if (query.toSeq() != null && e.seq() > query.toSeq()) break;
            String eventType = e.getClass().getSimpleName();
            if (query.eventTypes() != null && !query.eventTypes().isEmpty()
                    && !query.eventTypes().contains(eventType)) {
                continue;
            }
            result.add(new SessionEventView(
                    e.seq(),
                    eventType,
                    e.time(),
                    toMap(e)
            ));
        }
        return result;
    }

    /**
     * 处理to Map相关逻辑。
     */
    private java.util.Map<String, Object> toMap(SessionEvent e) {
        java.util.Map<String, Object> m = new LinkedHashMap<>();
        m.put("seq", e.seq());
        m.put("type", e.getClass().getSimpleName());
        m.put("time", e.time() != null ? e.time().toString() : null);
        return m;
    }
}
