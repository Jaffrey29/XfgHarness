package cn.xiaofuge.deepseek.harness.cases.task.submit.node;

import cn.xiaofuge.deepseek.harness.api.dto.task.SubmitTaskRequest;
import cn.xiaofuge.deepseek.harness.api.dto.task.SubmitTaskResponse;
import cn.xiaofuge.deepseek.harness.cases.task.submit.factory.SubmitTaskDynamicContext;
import cn.xiaofuge.deepseek.harness.domain.shared.strategy.StrategyHandler;
import cn.xiaofuge.deepseek.harness.domain.runtime.tool.model.valobj.ToolSelectionVO;
import cn.xiaofuge.deepseek.harness.domain.task.submission.model.valobj.TaskSubmissionVO;
import cn.xiaofuge.deepseek.harness.domain.task.ITaskSubmissionPolicyService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * 根节点：校验任务提交并初始化动态上下文。
 * <p>
 * 链路：RootNode → ProfileResolutionNode → ToolResolutionNode →
 *       PermissionCheckNode → EnqueueNode
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service("submitTaskRootNode")
public class SubmissionRootNode extends AbstractSubmitTaskNode {

    @Autowired
    private ITaskSubmissionPolicyService taskSubmissionPolicyService;

    @Autowired
    @Qualifier("submitTaskProfileResolutionNode")
    private ProfileResolutionNode profileResolutionNode;

    @Override
    protected SubmitTaskResponse doApply(SubmitTaskRequest request,
                                         SubmitTaskDynamicContext ctx) throws Exception {
        TaskSubmissionVO submission = new TaskSubmissionVO(
                request.workspaceId(),
                request.prompt(),
                request.profileCode(),
                new ToolSelectionVO(request.requestedTools())
        );
        taskSubmissionPolicyService.validate(submission);
        ctx.setSubmission(submission);
        return null; // continue to next node
    }

    @Override
    public StrategyHandler<SubmitTaskRequest, SubmitTaskDynamicContext, SubmitTaskResponse>
    getNext(SubmitTaskRequest request, SubmitTaskDynamicContext ctx) {
        return profileResolutionNode;
    }
}
