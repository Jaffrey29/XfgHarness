package cn.xiaofuge.deepseek.harness.domain.session;

/**
 * Owns append-only session event recording.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface ISessionLogService {

    void recordTaskSubmission(String sessionId, String taskId, String profileCode, String prompt, boolean approvalRequired, String approvalReason);

    void recordApprovalAccepted(String sessionId);

    void recordExecutionStarted(String sessionId, String resolvedModel);

    void recordExecutionCompleted(String sessionId, String modelCode, String assistantMessage);

    void recordExecutionFailed(String sessionId, String failureMessage);
}
