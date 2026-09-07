package cn.xiaofuge.deepseek.harness.domain.task.submission.service;

import cn.xiaofuge.deepseek.harness.domain.task.ITaskSubmissionPolicyService;
import cn.xiaofuge.deepseek.harness.domain.task.submission.model.valobj.TaskSubmissionVO;
import cn.xiaofuge.deepseek.harness.domain.task.submission.service.filter.TaskSubmissionFilter;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Applies the configured submission-validation chain.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class TaskSubmissionPolicyService implements ITaskSubmissionPolicyService {

    private final List<TaskSubmissionFilter> filters;

    /**
     * 处理Task Submission Policy Service相关逻辑。
     */
    public TaskSubmissionPolicyService(List<TaskSubmissionFilter> filters) {
        this.filters = List.copyOf(filters);
    }

    @Override
    public void validate(TaskSubmissionVO submission) {
        filters.forEach(filter -> filter.filter(submission));
    }
}
