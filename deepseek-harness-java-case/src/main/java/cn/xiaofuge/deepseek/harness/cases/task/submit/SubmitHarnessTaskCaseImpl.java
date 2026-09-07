package cn.xiaofuge.deepseek.harness.cases.task.submit;

import cn.xiaofuge.deepseek.harness.cases.task.SubmitTaskCommand;
import cn.xiaofuge.deepseek.harness.cases.task.SubmitTaskResult;
import cn.xiaofuge.deepseek.harness.cases.task.ISubmitHarnessTaskCase;
import cn.xiaofuge.deepseek.harness.cases.task.submit.factory.SubmitHarnessTaskFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 轻量入口：通过工厂委托给策略树处理。
 *
 * <p>调用流程：SubmissionRootNode → ProfileResolutionNode → ToolResolutionNode → PermissionCheckNode → EnqueueNode。</p>
 * <p>示例：提交一条任务后，先解析执行画像，再校验权限，最后进入待执行队列。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class SubmitHarnessTaskCaseImpl implements ISubmitHarnessTaskCase {

    @Autowired
    private SubmitHarnessTaskFactory factory;

    @Override
    public SubmitTaskResult execute(SubmitTaskCommand request) {
        try {
            return factory.strategyHandler().apply(request, factory.newContext());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
