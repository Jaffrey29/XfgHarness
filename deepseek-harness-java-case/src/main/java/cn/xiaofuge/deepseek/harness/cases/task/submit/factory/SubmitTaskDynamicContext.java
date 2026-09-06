package cn.xiaofuge.deepseek.harness.cases.task.submit.factory;

import cn.xiaofuge.deepseek.harness.domain.runtime.model.model.valobj.ModelProviderProfileVO;
import cn.xiaofuge.deepseek.harness.domain.runtime.profile.model.valobj.ExecutionProfileVO;
import cn.xiaofuge.deepseek.harness.domain.session.registry.model.valobj.SessionOpenedResultVO;
import cn.xiaofuge.deepseek.harness.domain.task.approval.model.valobj.ApprovalDecisionVO;
import cn.xiaofuge.deepseek.harness.domain.task.permission.model.valobj.PermissionAssessmentVO;
import cn.xiaofuge.deepseek.harness.domain.task.queue.model.valobj.QueuedTaskResultVO;
import cn.xiaofuge.deepseek.harness.domain.task.submission.model.valobj.TaskSubmissionVO;
import java.util.List;

/**
 * 任务提交策略树中流转的可变上下文。
 * 每个节点读取已累积状态，并写入自己的处理结果。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class SubmitTaskDynamicContext {

    private TaskSubmissionVO submission;
    private ExecutionProfileVO executionProfile;
    private ModelProviderProfileVO modelProfile;
    private List<String> enabledTools;
    private PermissionAssessmentVO permissionAssessment;
    private ApprovalDecisionVO approvalDecision;
    private SessionOpenedResultVO sessionResult;
    private QueuedTaskResultVO taskResult;
    private String currentStatus;

    public TaskSubmissionVO getSubmission() { return submission; }
    public void setSubmission(TaskSubmissionVO submission) { this.submission = submission; }

    public ExecutionProfileVO getExecutionProfile() { return executionProfile; }
    public void setExecutionProfile(ExecutionProfileVO executionProfile) { this.executionProfile = executionProfile; }

    public ModelProviderProfileVO getModelProfile() { return modelProfile; }
    public void setModelProfile(ModelProviderProfileVO modelProfile) { this.modelProfile = modelProfile; }

    public List<String> getEnabledTools() { return enabledTools; }
    public void setEnabledTools(List<String> enabledTools) { this.enabledTools = enabledTools; }

    public PermissionAssessmentVO getPermissionAssessment() { return permissionAssessment; }
    public void setPermissionAssessment(PermissionAssessmentVO permissionAssessment) { this.permissionAssessment = permissionAssessment; }

    public ApprovalDecisionVO getApprovalDecision() { return approvalDecision; }
    public void setApprovalDecision(ApprovalDecisionVO approvalDecision) { this.approvalDecision = approvalDecision; }

    public SessionOpenedResultVO getSessionResult() { return sessionResult; }
    public void setSessionResult(SessionOpenedResultVO sessionResult) { this.sessionResult = sessionResult; }

    public QueuedTaskResultVO getTaskResult() { return taskResult; }
    public void setTaskResult(QueuedTaskResultVO taskResult) { this.taskResult = taskResult; }

    public String getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }
}
