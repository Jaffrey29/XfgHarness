package cn.xiaofuge.deepseek.harness.cases.session.node;

import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.api.dto.SessionListRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.SessionQueryResponseDTO;
import cn.xiaofuge.deepseek.harness.domain.session.IConversationQueryService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 会话列表查询节点。
 * <p>流程：读取 workspaceId、offset、limit → 调用会话查询领域服务 → 转换为会话 DTO 列表，不附带消息。</p>
 * <p>数据示例：传入 {@code workspaceId=ws-1, offset=0, limit=50}；
 * 返回 {@code [{sessionId: "BLUE-8090", status: "ACTIVE", modelCode: "glm-5.3-flash"}]}。</p>
 */
@Service
public class SessionListNode
        extends AbstractStrategyRouter<SessionListRequestDTO, NoContext, List<SessionQueryResponseDTO>> {

    private static final Logger log = LoggerFactory.getLogger(SessionListNode.class);

    private final IConversationQueryService conversationQueryService;

    public SessionListNode(IConversationQueryService conversationQueryService) {
        this.conversationQueryService = conversationQueryService;
    }

    /**
     * 执行当前节点核心逻辑：校验输入、调用领域服务并返回输出。
     */
    @Override
    protected List<SessionQueryResponseDTO> doApply(SessionListRequestDTO query, NoContext context) {
        var result = conversationQueryService.querySessions(query.workspaceId(), query.offset(), query.limit())
                .stream()
                .map(s -> new SessionQueryResponseDTO(
                        s.sessionId(), s.workspaceId(), s.status(), s.modelCode(), s.profileCode(),
                        s.createdAt(), s.lastActivityAt(), null
                ))
                .toList();
        log.debug("[Session] 会话列表查询完成，workspaceId={} offset={} limit={} count={}",
                query.workspaceId(), query.offset(), query.limit(), result.size());
        return result;
    }

    /**
     * 返回下一执行节点；单节点链路返回默认终止处理。
     */
    @Override
    public StrategyHandler<SessionListRequestDTO, NoContext, List<SessionQueryResponseDTO>> getNext(
            SessionListRequestDTO query, NoContext context) {
        return defaultStrategyHandler;
    }
}
