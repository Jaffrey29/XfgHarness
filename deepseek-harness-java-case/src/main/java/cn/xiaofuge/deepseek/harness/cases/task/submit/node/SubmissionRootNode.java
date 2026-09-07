package cn.xiaofuge.deepseek.harness.cases.task.submit.node;

import cn.xiaofuge.deepseek.harness.api.dto.SubmitTaskRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.SubmitTaskResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.task.submit.factory.SubmitTaskDynamicContext;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.domain.runtime.tool.model.valobj.ToolSelectionVO;
import cn.xiaofuge.deepseek.harness.domain.task.submission.model.valobj.TaskSubmissionVO;
import cn.xiaofuge.deepseek.harness.domain.task.ITaskSubmissionPolicyService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 根节点：校验任务提交并初始化动态上下文。
 * <p>
 * 链路：RootNode → ProfileResolutionNode → ToolResolutionNode →
 *       PermissionCheckNode → EnqueueNode
 *
 * <p>调用流程：SubmissionRootNode → ProfileResolutionNode → ToolResolutionNode → PermissionCheckNode → EnqueueNode。</p>
 * <p>示例：提交一条任务后，先解析执行画像，再校验权限，最后进入待执行队列。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service("submitTaskRootNode")
public class SubmissionRootNode extends AbstractSubmitTaskNode {

    private static final Logger log = LoggerFactory.getLogger(SubmissionRootNode.class);

    @Autowired
    private ITaskSubmissionPolicyService taskSubmissionPolicyService;

    @Autowired
    @Qualifier("submitTaskProfileResolutionNode")
    private ProfileResolutionNode profileResolutionNode;

    /**
     * do Apply。
     * 流程：读取命令 → 执行用例编排 → 返回结果。
     * @param request 请求对象。
     * @param ctx ctx参数。
     * @return 方法执行结果。
     */

    @Override
    protected SubmitTaskResponseDTO doApply(SubmitTaskRequestDTO request,
                                         SubmitTaskDynamicContext ctx) throws Exception {
        log.info("[Submit] 收到任务提交请求（workspaceId={} profileCode={} 请求工具={} prompt长度={}）",
                request.workspaceId(), request.profileCode(),
                request.requestedTools(), request.prompt() == null ? 0 : request.prompt().length());
        TaskSubmissionVO submission = new TaskSubmissionVO(
                request.workspaceId(),
                request.prompt(),
                request.profileCode(),
                new ToolSelectionVO(request.requestedTools())
        );
        taskSubmissionPolicyService.validate(submission);
        ctx.setSubmission(submission);
        log.info("[Submit] 任务校验通过，进入下一节点", "");
        return null; // continue to next node
    }

    @Override
    public StrategyHandler<SubmitTaskRequestDTO, SubmitTaskDynamicContext, SubmitTaskResponseDTO>
    /**
     * 获取Next。
     * 流程：读取命令 → 执行用例编排 → 返回结果。
     * @param request 请求对象。
     * @param ctx ctx参数。
     * @return 获取到的Next。
     */

    getNext(SubmitTaskRequestDTO request, SubmitTaskDynamicContext ctx) {
        return profileResolutionNode;
    }
}
