package cn.xiaofuge.deepseek.harness.domain.task.submission.service.filter;

import cn.xiaofuge.deepseek.harness.domain.task.submission.model.valobj.TaskSubmissionVO;

/**
 * Rejects requests that do not pick an execution profile.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class ProfileCodeFilter implements TaskSubmissionFilter {

    @Override
    public void filter(TaskSubmissionVO submission) {
        if (submission.profileCode() == null || submission.profileCode().isBlank()) {
            throw new IllegalArgumentException("profileCode must not be blank");
        }
    }
}
