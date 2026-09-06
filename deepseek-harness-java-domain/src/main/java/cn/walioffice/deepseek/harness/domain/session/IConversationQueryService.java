package cn.walioffice.deepseek.harness.domain.session;

import cn.walioffice.deepseek.harness.domain.session.conversation.model.valobj.ConversationMessageVO;
import cn.walioffice.deepseek.harness.domain.session.conversation.model.valobj.ConversationSessionDetailVO;
import cn.walioffice.deepseek.harness.domain.session.conversation.model.valobj.ConversationSessionSummaryVO;
import java.util.List;

/**
 * 会话查询服务接口。
 * <p>
 * 执行流程：
 * 1. 上层请求会话摘要或消息；
 * 2. 领域层从持久化数据装配读模型；
 * 3. 返回 UI 可直接渲染的数据结构。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IConversationQueryService {

    List<ConversationSessionSummaryVO> listRecentSessions(int limit);

    List<ConversationMessageVO> listMessages(String sessionId);

    /**
     * 分页查询会话摘要。
     * <p>
     * 执行流程：
     * 1. 读取工作区和分页参数；
     * 2. 查询对应会话并截取范围；
     * 3. 返回分页读模型。
     */
    List<ConversationSessionSummaryVO> querySessions(String workspaceId, int offset, int limit);

    /**
     * 查询会话详情。
     * <p>
     * 执行流程：
     * 1. 读取 sessionId；
     * 2. 装配会话主数据和消息列表；
     * 3. 返回完整详情。
     */
    ConversationSessionDetailVO querySessionDetail(String sessionId);
}
