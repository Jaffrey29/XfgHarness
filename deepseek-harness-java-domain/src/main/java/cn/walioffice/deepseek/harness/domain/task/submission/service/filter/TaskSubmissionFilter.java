package cn.walioffice.deepseek.harness.domain.task.submission.service.filter;

import cn.walioffice.deepseek.harness.domain.task.submission.model.valobj.TaskSubmissionVO;

/**
 * Defines one validation step over a task submission request.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface TaskSubmissionFilter {

    /**
     * Validates one submission input.
     *
     * @param submission submission input
     */
    void filter(TaskSubmissionVO submission);
}
