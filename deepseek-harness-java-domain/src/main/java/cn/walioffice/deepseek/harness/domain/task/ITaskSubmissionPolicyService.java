package cn.walioffice.deepseek.harness.domain.task;

import cn.walioffice.deepseek.harness.domain.task.submission.model.valobj.TaskSubmissionVO;

/**
 * Owns submission validation before downstream domains resolve execution details.
 *
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
