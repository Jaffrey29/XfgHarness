package cn.xiaofuge.deepseek.harness.domain.task;

import cn.xiaofuge.deepseek.harness.domain.task.submission.model.valobj.TaskSubmissionVO;

/**
 * Owns submission validation before downstream domains resolve execution details.
 *
 * <p>调用流程：提交 → 权限评估 → 审批 → 队列 → 执行 → 状态回写。</p>
 * <p>示例：需要审批的命令先挂起，批准后进入执行队列。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface ITaskSubmissionPolicyService {

    /**
     * Validates one submission request.
     *
     * @param submission submission input
     */
    void validate(TaskSubmissionVO submission);
}
