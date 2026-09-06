package cn.walioffice.deepseek.harness.domain.session.event.service;

import cn.walioffice.deepseek.harness.domain.llm.model.entity.Message;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.TextBlock;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.TokenUsage;
import cn.walioffice.deepseek.harness.domain.llm.model.entity.MessageSource;
import cn.walioffice.deepseek.harness.domain.session.event.model.entity.SessionEvent;
import cn.walioffice.deepseek.harness.domain.session.event.model.valobj.*;
import java.util.List;
import java.util.Map;

/**
 * Bridges the new typed {@link SessionEvent} system with the legacy
 * {@link ISessionLogService} / {@code SessionEventEntity} API.
 * <p>
 * This adapter allows existing code (SubmitHarnessTaskCase, HarnessExecutionService, etc.)
 * to continue working while the new event system is rolled out.
 * <p>
 * It translates legacy {@code recordTaskSubmission} / {@code recordExecutionCompleted}
 * calls into typed session events.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class LegacyEventAdapter {

    private final ISessionEventLogService eventLogService;
    private final SurfaceProjector surfaceProjector;

    public LegacyEventAdapter(ISessionEventLogService eventLogService) {
        this.eventLogService = eventLogService;
        this.surfaceProjector = new SurfaceProjector();
    }

    /**
     * Record a task submission as session events.
     * Produces: user/message + task/submitted + [task/approval.required | task/auto-approved]
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
     * Record approval accepted.
     */
    public void recordApprovalAccepted(String sessionId) {
        eventLogService.append(sessionId, SessionEventFactory.generic(
                SessionEventType.TASK_APPROVED, "approved from web console"
        ));
    }

    /**
     * Record execution started.
     */
    public void recordExecutionStarted(String sessionId, String resolvedModel) {
        eventLogService.append(sessionId, SessionEventFactory.generic(
                SessionEventType.TASK_EXECUTION_STARTED, "model=" + resolvedModel
        ));
    }

    /**
     * Record execution completed: assistant/message + task.execution/completed.
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
     * Record execution failed: assistant/message + task.execution/failed.
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
     * Query visible messages for the conversation view.
     * Uses the surface projector to extract user + assistant messages.
     */
    public List<VisibleMessage> queryVisibleMessages(String sessionId) {
        List<SessionEvent> events = eventLogService.readAll(sessionId);
        List<SessionEvent> chatMessages = surfaceProjector.extractChatMessages(events);
        return chatMessages.stream()
                .map(this::toVisibleMessage)
                .toList();
    }

    // ── Helpers ──────────────────────────────────────────────────

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
     * Simple message projection for chat views.
     */
    public record VisibleMessage(String role, String content, java.time.Instant occurredAt) {}
}
