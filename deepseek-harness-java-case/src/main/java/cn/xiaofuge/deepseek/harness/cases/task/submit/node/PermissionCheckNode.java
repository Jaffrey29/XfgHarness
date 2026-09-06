package cn.xiaofuge.deepseek.harness.cases.task.submit.node;

import cn.xiaofuge.deepseek.harness.api.dto.task.SubmitTaskRequest;
import cn.xiaofuge.deepseek.harness.api.dto.task.SubmitTaskResponse;
import cn.xiaofuge.deepseek.harness.cases.task.submit.factory.SubmitTaskDynamicContext;
import cn.xiaofuge.deepseek.harness.domain.shared.StrategyHandler;
import cn.xiaofuge.deepseek.harness.domain.task.IApprovalPolicyService;
import cn.xiaofuge.deepseek.harness.domain.task.IPermissionPolicyService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 权限检查节点：评估权限并决定是否需要审批。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service("submitTaskPermissionCheckNode")
public class PermissionCheckNode extends AbstractSubmitTaskNode {

    private static final Logger log = LoggerFactory.getLogger(PermissionCheckNode.class);

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
        log.info("[Submit] 权限评估完成，启用工具={}", ctx.getPermissionAssessment().enabledTools());
        ctx.setApprovalDecision(approvalPolicyService.decide(
                ctx.getSubmission().profileCode(),
                ctx.getPermissionAssessment()
        ));
        log.info("[Submit] 审批决策完成，需审批={} 原因={}",
                ctx.getApprovalDecision().approvalRequired(), ctx.getApprovalDecision().reason());
        return null;
    }

    @Override
    public StrategyHandler<SubmitTaskRequest, SubmitTaskDynamicContext, SubmitTaskResponse>
    getNext(SubmitTaskRequest request, SubmitTaskDynamicContext ctx) {
        return enqueueNode;
    }
}
