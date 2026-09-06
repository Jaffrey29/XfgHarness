package cn.xiaofuge.deepseek.harness.cases.task.submit.node;

import cn.xiaofuge.deepseek.harness.api.dto.task.SubmitTaskRequest;
import cn.xiaofuge.deepseek.harness.api.dto.task.SubmitTaskResponse;
import cn.xiaofuge.deepseek.harness.cases.task.submit.factory.SubmitTaskDynamicContext;
import cn.xiaofuge.deepseek.harness.domain.shared.strategy.StrategyHandler;
import cn.xiaofuge.deepseek.harness.domain.runtime.IExecutionProfileService;
import cn.xiaofuge.deepseek.harness.domain.runtime.IModelRoutingService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * 档位解析节点：解析执行档位和模型路由。
 *
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

    @Override
    protected SubmitTaskResponse doApply(SubmitTaskRequest request,
                                         SubmitTaskDynamicContext ctx) throws Exception {
        var submission = ctx.getSubmission();
        ctx.setExecutionProfile(executionProfileService.resolve(submission.profileCode()));
        ctx.setModelProfile(modelRoutingService.resolve(submission.profileCode()));
        return null;
    }

    @Override
    public StrategyHandler<SubmitTaskRequest, SubmitTaskDynamicContext, SubmitTaskResponse>
    getNext(SubmitTaskRequest request, SubmitTaskDynamicContext ctx) {
        return toolResolutionNode;
    }
}
