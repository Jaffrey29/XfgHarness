package cn.xiaofuge.deepseek.harness.cases.task.submit.node;

import cn.xiaofuge.deepseek.harness.api.dto.task.SubmitTaskRequest;
import cn.xiaofuge.deepseek.harness.api.dto.task.SubmitTaskResponse;
import cn.xiaofuge.deepseek.harness.cases.task.submit.factory.SubmitTaskDynamicContext;
import cn.xiaofuge.deepseek.harness.domain.shared.StrategyHandler;
import cn.xiaofuge.deepseek.harness.domain.session.IHarnessSessionRegistryService;
import cn.xiaofuge.deepseek.harness.domain.session.ISessionLogService;
import cn.xiaofuge.deepseek.harness.domain.task.IHarnessExecutionService;
import cn.xiaofuge.deepseek.harness.domain.task.IHarnessTaskQueueService;
import cn.xiaofuge.deepseek.harness.domain.task.execution.model.valobj.RuntimeExecutionPlanVO;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;

/**
 * 入队节点：创建会话、任务入队、记录日志，并按条件继续执行。
 * 这是任务提交链路的终止节点。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service("submitTaskEnqueueNode")
public class EnqueueNode extends AbstractSubmitTaskNode {

    private static final Logger log = LoggerFactory.getLogger(EnqueueNode.class);

    @Autowired
    private IHarnessSessionRegistryService harnessSessionRegistryService;

    @Autowired
    private IHarnessTaskQueueService harnessTaskQueueService;

    @Autowired
    private ISessionLogService sessionLogService;

    @Autowired
    private IHarnessExecutionService harnessExecutionService;

    @Override
    protected SubmitTaskResponse doApply(SubmitTaskRequest request,
                                         SubmitTaskDynamicContext ctx) throws Exception {
        var submission = ctx.getSubmission();
        var profile = ctx.getExecutionProfile();
        var model = ctx.getModelProfile();
        var permission = ctx.getPermissionAssessment();
        var approval = ctx.getApprovalDecision();

        RuntimeExecutionPlanVO runtimePlan = new RuntimeExecutionPlanVO(
                submission.workspaceId(),
                submission.prompt(),
                profile.profileCode(),
                profile.executionMode(),
                model.modelCode(),
                permission.enabledTools(),
                Instant.now(),
                approval.status()
        );

        ctx.setSessionResult(harnessSessionRegistryService.openQueuedSession(runtimePlan));
        log.info("[Submit] 创建排队会话完成，sessionId={} resolvedModel={}",
                ctx.getSessionResult().sessionId(), ctx.getSessionResult().resolvedModel());
        ctx.setTaskResult(harnessTaskQueueService.createQueuedTask(ctx.getSessionResult().sessionId(), runtimePlan));
        ctx.setCurrentStatus(ctx.getTaskResult().status().name());
        log.info("[Submit] 任务入队完成，taskId={} status={}",
                ctx.getTaskResult().taskId(), ctx.getTaskResult().status().name());

        sessionLogService.recordTaskSubmission(
                ctx.getSessionResult().sessionId(),
                ctx.getTaskResult().taskId(),
                request.profileCode(),
                request.prompt(),
                approval.approvalRequired(),
                approval.reason()
        );

        if (!approval.approvalRequired()) {
            log.info("[Submit] 无需审批，立即触发执行 sessionId={}...", ctx.getSessionResult().sessionId());
            ctx.setCurrentStatus(harnessExecutionService
                    .executeSession(ctx.getSessionResult().sessionId()).status().name());
            log.info("[Submit] 执行触发完成，sessionId={} status={}", ctx.getSessionResult().sessionId(), ctx.getCurrentStatus());
        } else {
            log.info("[Submit] 需要审批，任务挂起等待审批 taskId={}", ctx.getTaskResult().taskId());
        }

        return new SubmitTaskResponse(
                ctx.getTaskResult().taskId(),
                ctx.getSessionResult().sessionId(),
                ctx.getCurrentStatus(),
                ctx.getSessionResult().resolvedModel(),
                ctx.getTaskResult().enabledTools(),
                approval.approvalRequired(),
                approval.reason(),
                ctx.getTaskResult().acceptedAt()
        );
    }

    @Override
    public StrategyHandler<SubmitTaskRequest, SubmitTaskDynamicContext, SubmitTaskResponse>
    getNext(SubmitTaskRequest request, SubmitTaskDynamicContext ctx) {
        return defaultStrategyHandler;
    }
}
