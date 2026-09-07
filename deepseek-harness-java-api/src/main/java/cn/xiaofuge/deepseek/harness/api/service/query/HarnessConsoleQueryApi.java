package cn.xiaofuge.deepseek.harness.api.service.query;

import cn.xiaofuge.deepseek.harness.api.response.conversation.ConversationMessageResponse;
import cn.xiaofuge.deepseek.harness.api.response.conversation.ConversationSessionSummaryResponse;
import cn.xiaofuge.deepseek.harness.domain.session.IConversationQueryService;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 会话查询适配器。
 * <p>
 * 执行流程：
 * 1. 接收 Trigger 层的会话查询请求；
 * 2. 调用领域查询服务读取会话摘要和消息；
 * 3. 映射成前端可直接使用的响应对象。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class HarnessConsoleQueryApi {

    private final IConversationQueryService conversationQueryService;

    /**
     * 处理Harness Console Query Api相关逻辑。
     */
    public HarnessConsoleQueryApi(IConversationQueryService conversationQueryService) {
        this.conversationQueryService = conversationQueryService;
    }

    /**
     * 查询最近的会话摘要。
     * <p>
     * 执行流程：
     * 1. 调用领域查询服务读取会话摘要；
     * 2. 转换为接口响应对象；
     * 3. 返回给前端侧边栏。
     */
    public List<ConversationSessionSummaryResponse> listRecentSessions(int limit) {
        return conversationQueryService.listRecentSessions(limit).stream()
                .map(session -> new ConversationSessionSummaryResponse(
                        session.sessionId(),
                        session.title(),
                        session.preview(),
                        session.status(),
                        session.createdAt(),
                        session.agentId()
                ))
                .toList();
    }

    /**
     * 查询单个会话消息列表。
     * <p>
     * 执行流程：
     * 1. 根据 sessionId 读取领域会话消息；
     * 2. 将消息转为响应对象；
     * 3. 返回给前端会话详情页。
     */
    public List<ConversationMessageResponse> listMessages(String sessionId) {
        return conversationQueryService.listMessages(sessionId).stream()
                .map(message -> new ConversationMessageResponse(message.role(), message.content(), message.occurredAt()))
                .toList();
    }
}
