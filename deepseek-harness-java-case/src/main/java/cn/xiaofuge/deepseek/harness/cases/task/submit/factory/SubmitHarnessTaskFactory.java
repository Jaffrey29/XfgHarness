package cn.xiaofuge.deepseek.harness.cases.task.submit.factory;

import cn.xiaofuge.deepseek.harness.api.dto.task.SubmitTaskRequest;
import cn.xiaofuge.deepseek.harness.api.dto.task.SubmitTaskResponse;
import cn.xiaofuge.deepseek.harness.cases.task.submit.node.SubmissionRootNode;
import cn.xiaofuge.deepseek.harness.domain.shared.strategy.StrategyHandler;
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

    public StrategyHandler<SubmitTaskRequest, SubmitTaskDynamicContext, SubmitTaskResponse> strategyHandler() {
        return rootNode;
    }

    public SubmitTaskDynamicContext newContext() {
        return new SubmitTaskDynamicContext();
    }
}
