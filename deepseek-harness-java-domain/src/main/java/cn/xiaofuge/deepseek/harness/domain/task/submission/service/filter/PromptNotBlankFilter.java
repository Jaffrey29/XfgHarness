package cn.xiaofuge.deepseek.harness.domain.task.submission.service.filter;

import cn.xiaofuge.deepseek.harness.domain.task.submission.model.valobj.TaskSubmissionVO;

/**
 * Rejects empty prompts before any domain state is created.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class PromptNotBlankFilter implements TaskSubmissionFilter {

    @Override
    public void filter(TaskSubmissionVO submission) {
        if (submission.prompt() == null || submission.prompt().isBlank()) {
            throw new IllegalArgumentException("prompt must not be blank");
        }
    }
}
