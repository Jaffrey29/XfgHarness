package cn.xiaofuge.deepseek.harness.cases.task.submit;

import cn.xiaofuge.deepseek.harness.api.dto.SubmitTaskRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.SubmitTaskResponseDTO;
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

    /**
     * 用例执行入口，组装 Pipeline 并返回处理结果。
     */
    @Override
    public SubmitTaskResponseDTO execute(SubmitTaskRequestDTO request) {
        return factory.pipeline().execute(request);
    }
}
