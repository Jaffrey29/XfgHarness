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
 * <p>调用流程：Controller/API → 会话用例 → SessionQueryService/SessionRebuilderService → SessionEventStore。</p>
 * <p>示例：按 sessionId 查询事件后，可重建完整消息时间线。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class SessionQueryCaseImpl implements ISessionQueryCase {

    private final IConversationQueryService conversationQueryService;

    /**
     * 处理会话 Query Case Impl。
     * 流程：读取命令 → 执行用例编排 → 返回结果。
     * @param conversationQueryService conversation Query Service参数。
     * @return 方法执行结果。
     */
    public SessionQueryCaseImpl(IConversationQueryService conversationQueryService) {
        this.conversationQueryService = conversationQueryService;
    }

    /**
     * 查询Session Detail。
     * 流程：读取命令 → 执行用例编排 → 返回结果。
     * @param query query参数。
     * @return 方法执行结果。
     */

    /**
     * 查询Session Detail。
     * 流程：读取命令 → 执行用例编排 → 返回结果。
     * @param query query参数。
     * @return 方法执行结果。
     */

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
    /**
     * 查询Session Detail。
     * 流程：读取命令 → 执行用例编排 → 返回结果。
     * @param query query参数。
     * @return 方法执行结果。
     */


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
