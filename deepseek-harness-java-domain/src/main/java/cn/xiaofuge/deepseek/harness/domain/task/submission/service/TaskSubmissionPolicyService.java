package cn.xiaofuge.deepseek.harness.domain.task.submission.service;

import cn.xiaofuge.deepseek.harness.domain.task.ITaskSubmissionPolicyService;
import cn.xiaofuge.deepseek.harness.domain.task.submission.model.valobj.TaskSubmissionVO;
import cn.xiaofuge.deepseek.harness.domain.task.submission.service.filter.TaskSubmissionFilter;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Applies the configured submission-validation chain.
 *
 * <p>调用流程：提交 → 权限评估 → 审批 → 队列 → 执行 → 状态回写。</p>
 * <p>示例：需要审批的命令先挂起，批准后进入执行队列。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class TaskSubmissionPolicyService implements ITaskSubmissionPolicyService {

    private final List<TaskSubmissionFilter> filters;

    /**
     * 处理任务 Submission 策略 Service。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param filters 查询过滤条件。
     * @return 方法执行结果。
     */
    public TaskSubmissionPolicyService(List<TaskSubmissionFilter> filters) {
        this.filters = List.copyOf(filters);
    }

    @Override
    public void validate(TaskSubmissionVO submission) {
        filters.forEach(filter -> filter.filter(submission));
    }
}
