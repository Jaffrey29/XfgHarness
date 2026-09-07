package cn.xiaofuge.deepseek.harness.cases.task.submit.node;

import cn.xiaofuge.deepseek.harness.cases.task.SubmitTaskCommand;
import cn.xiaofuge.deepseek.harness.cases.task.SubmitTaskResult;
import cn.xiaofuge.deepseek.harness.cases.task.submit.factory.SubmitTaskDynamicContext;
import cn.xiaofuge.deepseek.harness.domain.shared.strategy.AbstractStrategyRouter;

/**
 * 任务提交策略树节点的抽象基类。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public abstract class AbstractSubmitTaskNode
        extends AbstractStrategyRouter<SubmitTaskCommand, SubmitTaskDynamicContext, SubmitTaskResult> {
}
