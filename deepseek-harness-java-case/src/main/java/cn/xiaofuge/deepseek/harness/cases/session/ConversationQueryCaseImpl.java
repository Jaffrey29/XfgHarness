package cn.xiaofuge.deepseek.harness.cases.session;

import cn.xiaofuge.deepseek.harness.api.dto.ConversationMessageResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.ConversationSessionSummaryResponseDTO;

import cn.xiaofuge.deepseek.harness.cases.session.factory.SessionPipelineFactory;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 会话消息与最近会话查询编排实现。
 * <p>流程：按查询目标选择 Pipeline → 执行对应节点 → 返回会话摘要或消息 DTO 列表。</p>
 * <p>数据示例：{@code listRecentSessions(20)} 返回最近 20 个会话；
 * {@code listMessages("BLUE-8090")} 返回 {@code role=USER, content=记住我是蓝色} 等记录。</p>
 */
@Service
public class ConversationQueryCaseImpl implements IConversationQueryCase {
    private final SessionPipelineFactory sessionPipelineFactory;

    public ConversationQueryCaseImpl(SessionPipelineFactory sessionPipelineFactory) {
        this.sessionPipelineFactory = sessionPipelineFactory;
    }

    /**
     * 编排最近会话查询，将 limit 交给查询 Pipeline。
     */
    @Override
    public List<ConversationSessionSummaryResponseDTO> listRecentSessions(int limit) {
        return sessionPipelineFactory.recentSessionsPipeline().execute(limit);
    }

    /**
     * 编排会话消息查询，将 sessionId 交给消息 Pipeline。
     */
    @Override
    public List<ConversationMessageResponseDTO> listMessages(String sessionId) {
        return sessionPipelineFactory.messagesPipeline().execute(sessionId);
    }
}
