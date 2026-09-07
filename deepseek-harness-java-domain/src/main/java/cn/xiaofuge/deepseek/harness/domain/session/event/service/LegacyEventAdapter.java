package cn.xiaofuge.deepseek.harness.domain.session.event.service;

import cn.xiaofuge.deepseek.harness.domain.model.entity.Message;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.TextBlock;
import cn.xiaofuge.deepseek.harness.domain.model.entity.MessageSource;
import cn.xiaofuge.deepseek.harness.domain.session.ISessionEventLogService;
import cn.xiaofuge.deepseek.harness.domain.session.ISessionLogService;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionEvent;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.valobj.SessionEventType;

import java.util.List;
import org.springframework.stereotype.Service;

/**
 * LegacyEventAdapter 表示LegacyEventAdapter事件。
 */
@Service
public class LegacyEventAdapter {

    private final ISessionEventLogService eventLogService;
    private final SurfaceProjector surfaceProjector;

    /**
     * 处理Legacy 事件 Adapter。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param eventLogService 事件 Log Service参数。
     * @return 方法执行结果。
     */
    public LegacyEventAdapter(ISessionEventLogService eventLogService) {
        this.eventLogService = eventLogService;
        this.surfaceProjector = new SurfaceProjector();
    }

    /**
     * 处理recordTaskSubmission。
     * <p>流程：校验入参 → 执行领域动作 → 返回处理结果。</p>
     * @param sessionId 会话ID。
     * @param profileCode 画像profileCode。
     * @param approvalRequired approvalRequired。
     * @return 领域处理结果。
     */
    public void recordTaskSubmission(
            String sessionId, String taskId, String profileCode,
            String prompt, boolean approvalRequired, String approvalReason
    ) {
        eventLogService.append(sessionId, SessionEventFactory.userMessage(prompt));
        eventLogService.append(sessionId, SessionEventFactory.generic(
                SessionEventType.TASK_SUBMITTED,
                "taskId=" + taskId + ",profile=" + profileCode
        ));
        if (approvalRequired) {
            eventLogService.append(sessionId, SessionEventFactory.generic(
                    SessionEventType.TASK_APPROVAL_REQUIRED, approvalReason
            ));
        } else {
            eventLogService.append(sessionId, SessionEventFactory.generic(
                    SessionEventType.TASK_AUTO_APPROVED, approvalReason
            ));
        }
    }

    /**
     * 处理recordApprovalAccepted。
     * <p>流程：校验入参 → 执行领域动作 → 返回处理结果。</p>
     * @param sessionId 会话ID。
     * @return 领域处理结果。
     */
    public void recordApprovalAccepted(String sessionId) {
        eventLogService.append(sessionId, SessionEventFactory.generic(
                SessionEventType.TASK_APPROVED, "approved from web console"
        ));
    }

    /**
     * 处理recordExecutionStarted。
     * <p>流程：校验入参 → 执行领域动作 → 返回处理结果。</p>
     * @param sessionId 会话ID。
     * @return 领域处理结果。
     */
    public void recordExecutionStarted(String sessionId, String resolvedModel) {
        eventLogService.append(sessionId, SessionEventFactory.generic(
                SessionEventType.TASK_EXECUTION_STARTED, "model=" + resolvedModel
        ));
    }

    /**
     * 处理recordExecutionCompleted。
     * <p>流程：校验入参 → 执行领域动作 → 返回处理结果。</p>
     * @param sessionId 会话ID。
     * @param assistantMessage assistantMessage消息。
     * @return 领域处理结果。
     */
    public void recordExecutionCompleted(String sessionId, String modelCode, String assistantMessage) {
        Message msg = Message.createAssistant(
                List.of(new TextBlock(assistantMessage)),
                new MessageSource.ModelMessageSource("deepseek", modelCode)
        );
        eventLogService.append(sessionId, SessionEventFactory.assistantMessage(1, 1, msg, null));
        eventLogService.append(sessionId, SessionEventFactory.generic(
                SessionEventType.TASK_EXECUTION_COMPLETED, "model=" + modelCode
        ));
    }

    /**
     * 处理recordExecutionFailed。
     * <p>流程：校验入参 → 执行领域动作 → 返回处理结果。</p>
     * @param sessionId 会话ID。
     * @return 领域处理结果。
     */
    public void recordExecutionFailed(String sessionId, String failureMessage) {
        Message msg = Message.createAssistant(
                List.of(new TextBlock(failureMessage)),
                new MessageSource.ModelMessageSource("deepseek", "error")
        );
        eventLogService.append(sessionId, SessionEventFactory.assistantMessage(1, 1, msg, null));
        eventLogService.append(sessionId, SessionEventFactory.generic(
                SessionEventType.TASK_EXECUTION_FAILED, failureMessage
        ));
    }

    /**
     * 处理queryVisibleMessages消息列表。
     * <p>流程：校验入参 → 执行领域动作 → 返回处理结果。</p>
     * @param sessionId 会话ID。
     * @return 领域处理结果。
     */
    public List<VisibleMessage> queryVisibleMessages(String sessionId) {
        List<SessionEvent> events = eventLogService.readAll(sessionId);
        List<SessionEvent> chatMessages = surfaceProjector.extractChatMessages(events);
        return chatMessages.stream()
                .map(this::toVisibleMessage)
                .toList();
    }

    // ── Helpers ──────────────────────────────────────────────────

    /**
     * 转换为 Visible 消息。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param event 领域事件。
     * @return 转换后的目标对象。
     */
    private VisibleMessage toVisibleMessage(SessionEvent event) {
        if (event instanceof SessionEvent.UserMessage e) {
            Message msg = e.data().message();
            String text = extractText(msg);
            return new VisibleMessage("user", text, e.time());
        } else if (event instanceof SessionEvent.AssistantMessage e) {
            Message msg = e.data().message();
            String text = extractText(msg);
            return new VisibleMessage("assistant", text, e.time());
        }
        return null;
    }

    /**
     * 提取 Text。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param msg msg参数。
     * @return 方法执行结果。
     */
    private String extractText(Message msg) {
        if (msg == null || msg.content() == null) return "";
        StringBuilder sb = new StringBuilder();
        for (var block : msg.content()) {
            if (block instanceof TextBlock tb) {
                sb.append(tb.text());
            }
        }
        return sb.toString();
    }

    /**
     * VisibleMessage 表示VisibleMessage消息。
     */
    public record VisibleMessage(String role, String content, java.time.Instant occurredAt) {}
}
