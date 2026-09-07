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
 * <p>调用流程：Controller/API → 会话用例 → SessionQueryService/SessionRebuilderService → SessionEventStore。</p>
 * <p>示例：按 sessionId 查询事件后，可重建完整消息时间线。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class SessionEventQueryCaseImpl implements ISessionEventQueryCase {

    private final ISessionEventLogService sessionEventLogService;

    /**
     * 处理会话 事件 Query Case Impl。
     * 流程：读取命令 → 执行用例编排 → 返回结果。
     * @param sessionEventLogService 会话 事件 Log Service参数。
     * @return 方法执行结果。
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
     * 转换为 映射。
     * 流程：读取命令 → 执行用例编排 → 返回结果。
     * @param e 异常对象。
     * @return 转换后的目标对象。
     */
    private java.util.Map<String, Object> toMap(SessionEvent e) {
        java.util.Map<String, Object> m = new LinkedHashMap<>();
        m.put("seq", e.seq());
        m.put("type", e.getClass().getSimpleName());
        m.put("time", e.time() != null ? e.time().toString() : null);
        return m;
    }
}
