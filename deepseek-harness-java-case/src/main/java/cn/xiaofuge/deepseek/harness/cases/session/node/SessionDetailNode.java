package cn.xiaofuge.deepseek.harness.cases.session.node;

import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.api.dto.SessionDetailRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.SessionQueryResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.support.CaseGuard;
import cn.xiaofuge.deepseek.harness.domain.session.IConversationQueryService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 会话详情查询节点。
 * <p>流程：校验 sessionId 非空 → 查询单个会话摘要和消息 → 组装为带消息时间线的会话 DTO。</p>
 * <p>数据示例：传入 {@code sessionId=BLUE-8090}；返回 {@code status=ACTIVE} 和
 * {@code messages=[{role: "USER", content: "记住我是蓝色"}]}。</p>
 */
@Service
public class SessionDetailNode
        extends AbstractStrategyRouter<SessionDetailRequestDTO, NoContext, SessionQueryResponseDTO> {

    private static final Logger log = LoggerFactory.getLogger(SessionDetailNode.class);

    private final IConversationQueryService conversationQueryService;

    public SessionDetailNode(IConversationQueryService conversationQueryService) {
        this.conversationQueryService = conversationQueryService;
    }

    /**
     * 执行当前节点核心逻辑：校验输入、调用领域服务并返回输出。
     */
    @Override
    protected SessionQueryResponseDTO doApply(SessionDetailRequestDTO query, NoContext context) {
        CaseGuard.requireSessionId(query.sessionId());
        var summary = conversationQueryService.querySessionDetail(query.sessionId());
        List<Map<String, Object>> messages = new ArrayList<>();
        if (summary.messages() != null) {
            for (var message : summary.messages()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("role", message.role());
                item.put("content", message.content());
                messages.add(item);
            }
        }
        var result = new SessionQueryResponseDTO(
                summary.sessionId(), summary.workspaceId(), summary.status(), summary.modelCode(),
                summary.profileCode(), summary.createdAt(), summary.lastActivityAt(), messages
        );
        log.debug("[Session] 会话详情查询完成，sessionId={} messageCount={}",
                query.sessionId(), messages.size());
        return result;
    }

    /**
     * 返回下一执行节点；单节点链路返回默认终止处理。
     */
    @Override
    public StrategyHandler<SessionDetailRequestDTO, NoContext, SessionQueryResponseDTO> getNext(
            SessionDetailRequestDTO query, NoContext context) {
        return defaultStrategyHandler;
    }
}
