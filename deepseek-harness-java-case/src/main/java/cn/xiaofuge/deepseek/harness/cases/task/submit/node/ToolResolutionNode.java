package cn.xiaofuge.deepseek.harness.cases.task.submit.node;

import cn.xiaofuge.deepseek.harness.cases.task.SubmitTaskCommand;
import cn.xiaofuge.deepseek.harness.cases.task.SubmitTaskResult;
import cn.xiaofuge.deepseek.harness.cases.task.submit.factory.SubmitTaskDynamicContext;
import cn.xiaofuge.deepseek.harness.domain.shared.StrategyHandler;
import cn.xiaofuge.deepseek.harness.domain.runtime.IToolResolutionService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * 工具解析节点：解析启用的工具列表。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service("submitTaskToolResolutionNode")
public class ToolResolutionNode extends AbstractSubmitTaskNode {

    @Autowired
    private IToolResolutionService toolResolutionService;

    @Autowired
    @Qualifier("submitTaskPermissionCheckNode")
    private PermissionCheckNode permissionCheckNode;

    @Override
    protected SubmitTaskResult doApply(SubmitTaskCommand request,
                                         SubmitTaskDynamicContext ctx) throws Exception {
        var submission = ctx.getSubmission();
        ctx.setEnabledTools(toolResolutionService.resolveEnabledTools(submission.toolSelection().toolCodes()));
        return null;
    }

    @Override
    public StrategyHandler<SubmitTaskCommand, SubmitTaskDynamicContext, SubmitTaskResult>
    getNext(SubmitTaskCommand request, SubmitTaskDynamicContext ctx) {
        return permissionCheckNode;
    }
}
