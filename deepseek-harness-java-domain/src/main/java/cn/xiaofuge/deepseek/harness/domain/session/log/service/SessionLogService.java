package cn.xiaofuge.deepseek.harness.domain.session.log.service;

import cn.xiaofuge.deepseek.harness.domain.session.ISessionLogService;
import cn.xiaofuge.deepseek.harness.domain.session.log.adapter.repository.ISessionEventRepository;
import cn.xiaofuge.deepseek.harness.domain.session.log.model.entity.SessionEventEntity;
import java.time.Instant;

/**
 * Owns append-only session event recording.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class SessionLogService implements ISessionLogService {

    private final ISessionEventRepository sessionEventRepository;

    public SessionLogService(ISessionEventRepository sessionEventRepository) {
        this.sessionEventRepository = sessionEventRepository;
    }

    @Override
    public void recordTaskSubmission(String sessionId, String taskId, String profileCode, String prompt, boolean approvalRequired, String approvalReason) {
        Instant eventTime = Instant.now();
        sessionEventRepository.append(new SessionEventEntity(sessionId, "task.submitted", "taskId=" + taskId + ",profile=" + profileCode, eventTime));
        sessionEventRepository.append(new SessionEventEntity(sessionId, "user.message", prompt, eventTime));
        if (approvalRequired) {
            sessionEventRepository.append(new SessionEventEntity(sessionId, "assistant.message", "任务已提交，等待审批。" + approvalReason, eventTime));
            sessionEventRepository.append(new SessionEventEntity(sessionId, "task.approval.required", approvalReason, eventTime));
            return;
        }
        sessionEventRepository.append(new SessionEventEntity(sessionId, "task.auto.approved", approvalReason, eventTime));
    }

    @Override
    public void recordApprovalAccepted(String sessionId) {
        Instant eventTime = Instant.now();
        sessionEventRepository.append(new SessionEventEntity(sessionId, "task.approved", "approved from web console", eventTime));
    }

    @Override
    public void recordExecutionStarted(String sessionId, String resolvedModel) {
        Instant eventTime = Instant.now();
        sessionEventRepository.append(new SessionEventEntity(sessionId, "task.execution.started", "model=" + resolvedModel, eventTime));
    }

    @Override
    public void recordExecutionCompleted(String sessionId, String modelCode, String assistantMessage) {
        Instant eventTime = Instant.now();
        sessionEventRepository.append(new SessionEventEntity(sessionId, "assistant.message", assistantMessage, eventTime));
        sessionEventRepository.append(new SessionEventEntity(sessionId, "task.execution.completed", "model=" + modelCode, eventTime));
    }

    @Override
    public void recordExecutionFailed(String sessionId, String failureMessage) {
        Instant eventTime = Instant.now();
        sessionEventRepository.append(new SessionEventEntity(sessionId, "assistant.message", failureMessage, eventTime));
        sessionEventRepository.append(new SessionEventEntity(sessionId, "task.execution.failed", failureMessage, eventTime));
    }
}
