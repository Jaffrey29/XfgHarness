package cn.walioffice.deepseek.harness.domain.session.conversation.service;

import cn.walioffice.deepseek.harness.domain.session.IConversationQueryService;
import cn.walioffice.deepseek.harness.domain.session.conversation.model.valobj.ConversationMessageVO;
import cn.walioffice.deepseek.harness.domain.session.conversation.model.valobj.ConversationSessionSummaryVO;
import cn.walioffice.deepseek.harness.domain.session.event.adapter.repository.ISessionEventStore;
import cn.walioffice.deepseek.harness.domain.session.event.model.entity.SessionEvent;
import cn.walioffice.deepseek.harness.domain.session.event.model.entity.SessionHeader;
import cn.walioffice.deepseek.harness.domain.session.log.adapter.repository.ISessionEventRepository;
import cn.walioffice.deepseek.harness.domain.session.log.model.entity.SessionEventEntity;
import cn.walioffice.deepseek.harness.domain.session.registry.adapter.repository.IHarnessSessionRepository;
import cn.walioffice.deepseek.harness.domain.session.registry.model.aggregate.HarnessSessionAggregate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话查询服务。
 * <p>
 * 执行流程：
 * 1. 从会话仓储和事件仓储读取原始数据；
 * 2. 过滤并装配出侧边栏摘要、消息列表和详情；
 * 3. 返回 UI 可直接消费的读模型。
 * <p>
 * 双轨合并：Agent 会话（harness_session_event_log）与任务会话
 * （harness_session + harness_session_event）都会被聚合展示。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class ConversationQueryService implements IConversationQueryService {

    private final IHarnessSessionRepository harnessSessionRepository;
    private final ISessionEventRepository sessionEventRepository;
    private final ISessionEventStore sessionEventStore;

    public ConversationQueryService(
            IHarnessSessionRepository harnessSessionRepository,
            ISessionEventRepository sessionEventRepository,
            ISessionEventStore sessionEventStore
    ) {
        this.harnessSessionRepository = harnessSessionRepository;
        this.sessionEventRepository = sessionEventRepository;
        this.sessionEventStore = sessionEventStore;
    }

    /**
     * 查询最近会话。
     * <p>
     * 执行流程：
     * 1. 从旧会话表（harness_session）读取任务会话；
     * 2. 从新事件表（harness_session_header）读取 Agent 会话；
     * 3. 按创建时间倒序合并去重，装配摘要视图并返回。
     */
    @Override
    public List<ConversationSessionSummaryVO> listRecentSessions(int limit) {
        Map<String, ConversationSessionSummaryVO> byId = new LinkedHashMap<>();
        // 旧链路：任务会话
        for (HarnessSessionAggregate session : harnessSessionRepository.listRecent(limit)) {
            byId.put(session.getSessionId(), toSummary(session));
        }
        // 新链路：Agent 会话（以 agentPreset 为 agentId 还原侧边栏标识）
        for (SessionHeader header : sessionEventStore.listHeaders(limit)) {
            byId.computeIfAbsent(header.sessionId(), sid -> toSummaryFromHeader(header));
        }
        List<ConversationSessionSummaryVO> merged = new ArrayList<>(byId.values());
        merged.sort(Comparator.comparing(ConversationSessionSummaryVO::createdAt).reversed());
        return merged.stream().limit(limit).toList();
    }

    /**
     * 查询会话消息。
     * <p>
     * 执行流程：
     * 1. 优先从新事件表读取（Agent 会话）；
     * 2. 若无则回退到旧事件表（任务会话）；
     * 3. 过滤可见消息并转换为消息 VO。
     */
    @Override
    public List<ConversationMessageVO> listMessages(String sessionId) {
        List<SessionEvent> typed = sessionEventStore.readAll(sessionId);
        if (!typed.isEmpty()) {
            return typed.stream()
                    .map(this::toMessageFromEvent)
                    .filter(java.util.Objects::nonNull)
                    .toList();
        }
        return sessionEventRepository.queryBySessionId(sessionId).stream()
                .filter(this::isVisibleMessage)
                .map(this::toMessage)
                .toList();
    }

    /**
     * 分页查询会话摘要。
     * <p>
     * 执行流程：
     * 1. 根据 workspaceId 读取会话集合；
     * 2. 按 offset 和 limit 截取；
     * 3. 返回管理员视角摘要列表。
     */
    @Override
    public List<ConversationSessionSummaryVO> querySessions(String workspaceId, int offset, int limit) {
        List<HarnessSessionAggregate> sessions = harnessSessionRepository.listRecent(limit);
        return sessions.stream()
                .filter(s -> workspaceId == null || workspaceId.equals(s.getWorkspaceId()))
                .skip(offset)
                .limit(limit)
                .map(this::toSummaryWithAdmin)
                .toList();
    }

    /**
     * 查询会话详情。
     * <p>
     * 执行流程：
     * 1. 根据 sessionId 读取会话；
     * 2. 组装会话基本信息和消息列表；
     * 3. 返回完整详情对象。
     */
    @Override
    public cn.walioffice.deepseek.harness.domain.session.conversation.model.valobj.ConversationSessionDetailVO querySessionDetail(String sessionId) {
        HarnessSessionAggregate session = harnessSessionRepository.findBySessionId(sessionId).orElse(null);
        if (session == null) return null;
        List<ConversationMessageVO> messages = listMessages(sessionId);
        return new cn.walioffice.deepseek.harness.domain.session.conversation.model.valobj.ConversationSessionDetailVO(
                session.getSessionId(),
                session.getWorkspaceId(),
                session.getStatus().name(),
                session.getModelCode(),
                session.getProfileCode(),
                session.getCreatedAt(),
                session.getLastActivityAt(),
                messages
        );
    }

    /**
     * 装配会话摘要。
     * <p>
     * 执行流程：
     * 1. 读取会话消息；
     * 2. 取首条用户消息作为标题、末条消息作为摘要；
     * 3. 组装摘要读模型。
     */
    private ConversationSessionSummaryVO toSummary(HarnessSessionAggregate session) {
        List<ConversationMessageVO> messages = listMessages(session.getSessionId());
        String title = messages.stream()
                .filter(message -> "user".equals(message.role()))
                .findFirst()
                .map(message -> abbreviate(message.content(), 16))
                .orElse("新会话");
        String preview = messages.isEmpty() ? "等待消息" : abbreviate(messages.get(messages.size() - 1).content(), 24);
        return new ConversationSessionSummaryVO(
                session.getSessionId(),
                title,
                preview,
                session.getStatus().name(),
                session.getCreatedAt()
        );
    }

    /**
     * 装配管理员视角会话摘要。
     * <p>
     * 执行流程：
     * 1. 读取会话消息；
     * 2. 生成标题、摘要和额外元数据；
     * 3. 返回管理员侧读模型。
     */
    private ConversationSessionSummaryVO toSummaryWithAdmin(HarnessSessionAggregate session) {
        List<ConversationMessageVO> messages = listMessages(session.getSessionId());
        String title = messages.stream()
                .filter(message -> "user".equals(message.role()))
                .findFirst()
                .map(message -> abbreviate(message.content(), 16))
                .orElse("新会话");
        String preview = messages.isEmpty() ? "等待消息" : abbreviate(messages.get(messages.size() - 1).content(), 24);
        return new ConversationSessionSummaryVO(
                session.getSessionId(),
                title,
                preview,
                session.getStatus().name(),
                session.getCreatedAt(),
                session.getWorkspaceId(),
                session.getModelCode(),
                session.getProfileCode(),
                session.getLastActivityAt()
        );
    }

    private boolean isVisibleMessage(SessionEventEntity event) {
        return "user.message".equals(event.eventType()) || "assistant.message".equals(event.eventType());
    }

    private ConversationMessageVO toMessage(SessionEventEntity event) {
        String role = event.eventType().startsWith("user") ? "user" : "assistant";
        return new ConversationMessageVO(role, event.payload(), event.occurredAt());
    }

    /**
     * 装配新事件表会话摘要（Agent 会话）。
     * <p>
     * 执行流程：
     * 1. 从事件表读取全部事件；
     * 2. 取首条 user/message 文本作为标题、末条 user/assistant 文本作为摘要；
     * 3. 组装摘要读模型，agentId 从 agentPreset 还原（AgentFactory 创建时写入）。
     */
    private ConversationSessionSummaryVO toSummaryFromHeader(SessionHeader header) {
        List<SessionEvent> events = sessionEventStore.readAll(header.sessionId());
        String title = null;
        String preview = null;
        for (SessionEvent event : events) {
            String text = null;
            boolean isUser = false;
            if (event instanceof SessionEvent.UserMessage um) {
                text = extractText(um.data().message());
                isUser = true;
            } else if (event instanceof SessionEvent.AssistantMessage am) {
                text = extractText(am.data().message());
            }
            if (text == null || text.isBlank()) continue;
            if (isUser && title == null) {
                title = abbreviate(text, 16);
            }
            preview = abbreviate(text, 24);
        }
        return new ConversationSessionSummaryVO(
                header.sessionId(),
                title != null ? title : "新会话",
                preview != null ? preview : "等待消息",
                "COMPLETED",
                header.createdAt(),
                header.cwd(),
                null,
                null,
                null,
                header.agentPreset()
        );
    }

    /**
     * 将新事件表的单条事件转换为消息 VO。
     * 只暴露 user/message 与 assistant/message，其余事件返回 null 被过滤。
     */
    private ConversationMessageVO toMessageFromEvent(SessionEvent event) {
        if (event instanceof SessionEvent.UserMessage um) {
            return new ConversationMessageVO("user", extractText(um.data().message()), event.time());
        } else if (event instanceof SessionEvent.AssistantMessage am) {
            return new ConversationMessageVO("assistant", extractText(am.data().message()), event.time());
        }
        return null;
    }

    private String extractText(cn.walioffice.deepseek.harness.domain.llm.model.entity.Message message) {
        if (message == null || message.content() == null) return "";
        StringBuilder sb = new StringBuilder();
        for (var block : message.content()) {
            if (block instanceof cn.walioffice.deepseek.harness.domain.llm.model.valobj.TextBlock tb) {
                sb.append(tb.text());
            }
        }
        return sb.toString();
    }

    private String abbreviate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
