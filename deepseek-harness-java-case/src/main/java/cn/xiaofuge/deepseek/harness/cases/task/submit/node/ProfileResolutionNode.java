package cn.xiaofuge.deepseek.harness.cases.task.submit.node;

import cn.xiaofuge.deepseek.harness.api.dto.SubmitTaskRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.SubmitTaskResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.task.submit.factory.SubmitTaskDynamicContext;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.domain.runtime.IExecutionProfileService;
import cn.xiaofuge.deepseek.harness.domain.runtime.IModelRoutingService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * 档位解析节点：解析执行档位和模型路由。
 *
 * <p>调用流程：SubmissionRootNode → ProfileResolutionNode → ToolResolutionNode → PermissionCheckNode → EnqueueNode。</p>
 * <p>示例：提交一条任务后，先解析执行画像，再校验权限，最后进入待执行队列。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service("submitTaskProfileResolutionNode")
public class ProfileResolutionNode extends AbstractSubmitTaskNode {

    @Autowired
    private IExecutionProfileService executionProfileService;

    @Autowired
    private IModelRoutingService modelRoutingService;

    @Autowired
    @Qualifier("submitTaskToolResolutionNode")
    private ToolResolutionNode toolResolutionNode;

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
        var submission = ctx.getSubmission();
        ctx.setExecutionProfile(executionProfileService.resolve(submission.profileCode()));
        ctx.setModelProfile(modelRoutingService.resolve(submission.profileCode()));
        return null;
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
        return toolResolutionNode;
    }
}
