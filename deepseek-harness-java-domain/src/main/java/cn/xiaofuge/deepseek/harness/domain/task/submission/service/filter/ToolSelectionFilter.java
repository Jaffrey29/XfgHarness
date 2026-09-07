package cn.xiaofuge.deepseek.harness.domain.task.submission.service.filter;

import cn.xiaofuge.deepseek.harness.domain.task.submission.model.valobj.TaskSubmissionVO;
import org.springframework.stereotype.Service;

/**
 * Rejects malformed tool codes before tool resolution.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class ToolSelectionFilter implements TaskSubmissionFilter {

    @Override
    public void filter(TaskSubmissionVO submission) {
        boolean invalid = submission.toolSelection().toolCodes().stream()
                .anyMatch(toolCode -> toolCode == null || toolCode.isBlank());
        if (invalid) {
            throw new IllegalArgumentException("requestedTools contains blank tool code");
        }
    }
}
