package cn.xiaofuge.deepseek.harness.cases.session.node;

import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.api.dto.ConversationSessionSummaryResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.SessionListRequestDTO;
import cn.xiaofuge.deepseek.harness.domain.session.IConversationQueryService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 最近会话查询节点。
 * <p>流程：校验/透传 limit → 调用会话查询领域服务 → 转换为会话摘要 DTO 列表。</p>
 * <p>数据示例：传入 {@code limit=20}；返回 {@code [{sessionId: "BLUE-8090", title: "Memory demo", status: "ACTIVE"}]}。</p>
 */
@Service
public class ConversationRecentSessionsNode
        extends AbstractStrategyRouter<SessionListRequestDTO, NoContext, List<ConversationSessionSummaryResponseDTO>> {

    private static final Logger log = LoggerFactory.getLogger(ConversationRecentSessionsNode.class);

    private final IConversationQueryService conversationQueryService;

    public ConversationRecentSessionsNode(IConversationQueryService conversationQueryService) {
        this.conversationQueryService = conversationQueryService;
    }

    /**
     * 执行当前节点核心逻辑：校验输入、调用领域服务并返回输出。
     */
    @Override
    protected List<ConversationSessionSummaryResponseDTO> doApply(SessionListRequestDTO query, NoContext context) {
        var result = conversationQueryService.listRecentSessions(query.limit(), query.offset()).stream()
                .map(summary -> new ConversationSessionSummaryResponseDTO(
                        summary.sessionId(), summary.title(), summary.preview(), summary.status(),
                        summary.createdAt(), summary.agentId()
                ))
                .toList();
        log.debug("[Session] 最近会话查询完成，limit={} offset={} count={}", query.limit(), query.offset(), result.size());
        return result;
    }

    /**
     * 返回下一执行节点；单节点链路返回默认终止处理。
     */
    @Override
    public StrategyHandler<SessionListRequestDTO, NoContext, List<ConversationSessionSummaryResponseDTO>> getNext(
            SessionListRequestDTO query, NoContext context) {
        return defaultStrategyHandler;
    }
}
