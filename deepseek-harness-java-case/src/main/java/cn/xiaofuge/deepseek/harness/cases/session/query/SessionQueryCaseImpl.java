package cn.xiaofuge.deepseek.harness.cases.session.query;

import cn.xiaofuge.deepseek.harness.cases.session.ISessionQueryCase;
import cn.xiaofuge.deepseek.harness.cases.session.SessionDetailQuery;
import cn.xiaofuge.deepseek.harness.cases.session.SessionListQuery;
import cn.xiaofuge.deepseek.harness.cases.session.SessionView;
import cn.xiaofuge.deepseek.harness.domain.session.IConversationQueryService;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 薄委托到 {@link IConversationQueryService}。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class SessionQueryCaseImpl implements ISessionQueryCase {

    private final IConversationQueryService conversationQueryService;

    public SessionQueryCaseImpl(IConversationQueryService conversationQueryService) {
        this.conversationQueryService = conversationQueryService;
    }

    @Override
    public List<SessionView> querySessionList(SessionListQuery query) {
        return conversationQueryService.querySessions(
                        query.workspaceId(), query.offset(), query.limit())
                .stream()
                .map(s -> new SessionView(
                        s.sessionId(),
                        s.workspaceId(),
                        s.status(),
                        s.modelCode(),
                        s.profileCode(),
                        s.createdAt(),
                        s.lastActivityAt(),
                        null
                ))
                .toList();
    }

    @Override
    public SessionView querySessionDetail(SessionDetailQuery query) {
        var s = conversationQueryService.querySessionDetail(query.sessionId());
        List<java.util.Map<String, Object>> messages = new java.util.ArrayList<>();
        if (s.messages() != null) {
            for (var msg : s.messages()) {
                java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("role", msg.role());
                m.put("content", msg.content());
                messages.add(m);
            }
        }
        return new SessionView(
                s.sessionId(),
                s.workspaceId(),
                s.status(),
                s.modelCode(),
                s.profileCode(),
                s.createdAt(),
                s.lastActivityAt(),
                messages
        );
    }
}
