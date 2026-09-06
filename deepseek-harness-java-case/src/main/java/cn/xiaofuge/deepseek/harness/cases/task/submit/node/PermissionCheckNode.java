package cn.xiaofuge.deepseek.harness.cases.task.submit.node;

import cn.xiaofuge.deepseek.harness.api.dto.task.SubmitTaskRequest;
import cn.xiaofuge.deepseek.harness.api.dto.task.SubmitTaskResponse;
import cn.xiaofuge.deepseek.harness.cases.task.submit.factory.SubmitTaskDynamicContext;
import cn.xiaofuge.deepseek.harness.domain.shared.strategy.StrategyHandler;
import cn.xiaofuge.deepseek.harness.domain.task.IApprovalPolicyService;
import cn.xiaofuge.deepseek.harness.domain.task.IPermissionPolicyService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * 权限检查节点：评估权限并决定是否需要审批。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service("submitTaskPermissionCheckNode")
public class PermissionCheckNode extends AbstractSubmitTaskNode {

    @Autowired
    private IPermissionPolicyService permissionPolicyService;

    @Autowired
    private IApprovalPolicyService approvalPolicyService;

    @Autowired
    @Qualifier("submitTaskEnqueueNode")
    private EnqueueNode enqueueNode;

    @Override
    protected SubmitTaskResponse doApply(SubmitTaskRequest request,
                                         SubmitTaskDynamicContext ctx) throws Exception {
        ctx.setPermissionAssessment(permissionPolicyService.assess(ctx.getEnabledTools()));
        ctx.setApprovalDecision(approvalPolicyService.decide(
                ctx.getSubmission().profileCode(),
                ctx.getPermissionAssessment()
        ));
        return null;
    }

    @Override
    public StrategyHandler<SubmitTaskRequest, SubmitTaskDynamicContext, SubmitTaskResponse>
    getNext(SubmitTaskRequest request, SubmitTaskDynamicContext ctx) {
        return enqueueNode;
    }
}
