package cn.xiaofuge.deepseek.harness.cases.task.submit.node;

import cn.xiaofuge.deepseek.harness.api.dto.task.SubmitTaskRequest;
import cn.xiaofuge.deepseek.harness.api.dto.task.SubmitTaskResponse;
import cn.xiaofuge.deepseek.harness.cases.task.submit.factory.SubmitTaskDynamicContext;
import cn.xiaofuge.deepseek.harness.domain.shared.strategy.AbstractStrategyRouter;

/**
 * 任务提交策略树节点的抽象基类。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public abstract class AbstractSubmitTaskNode
        extends AbstractStrategyRouter<SubmitTaskRequest, SubmitTaskDynamicContext, SubmitTaskResponse> {
}
