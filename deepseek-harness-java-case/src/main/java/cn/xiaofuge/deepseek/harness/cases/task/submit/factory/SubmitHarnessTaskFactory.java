package cn.xiaofuge.deepseek.harness.cases.task.submit.factory;

import cn.xiaofuge.deepseek.harness.cases.task.SubmitTaskCommand;
import cn.xiaofuge.deepseek.harness.cases.task.SubmitTaskResult;
import cn.xiaofuge.deepseek.harness.cases.task.submit.node.SubmissionRootNode;
import cn.xiaofuge.deepseek.harness.domain.shared.StrategyHandler;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * 任务提交策略树工厂。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class SubmitHarnessTaskFactory {

    @Autowired
    @Qualifier("submitTaskRootNode")
    private SubmissionRootNode rootNode;

    public StrategyHandler<SubmitTaskCommand, SubmitTaskDynamicContext, SubmitTaskResult> strategyHandler() {
        return rootNode;
    }

    public SubmitTaskDynamicContext newContext() {
        return new SubmitTaskDynamicContext();
    }
}
