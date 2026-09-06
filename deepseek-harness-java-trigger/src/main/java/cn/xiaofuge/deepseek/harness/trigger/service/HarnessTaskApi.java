package cn.xiaofuge.deepseek.harness.trigger.service;

import cn.xiaofuge.deepseek.harness.api.dto.task.SubmitTaskRequest;
import cn.xiaofuge.deepseek.harness.api.dto.task.SubmitTaskResponse;
import cn.xiaofuge.deepseek.harness.cases.task.submit.ISubmitHarnessTaskCase;
import cn.xiaofuge.deepseek.harness.api.service.IHarnessTaskApi;
import org.springframework.stereotype.Service;

/**
 * 任务提交适配器。
 * <p>
 * 执行流程：
 * 1. 接收 Trigger 层提交的任务请求 DTO；
 * 2. 交给任务提交用例完成领域编排；
 * 3. 返回任务提交结果。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class HarnessTaskApi implements IHarnessTaskApi {

    private final ISubmitHarnessTaskCase submitHarnessTaskCase;

    public HarnessTaskApi(ISubmitHarnessTaskCase submitHarnessTaskCase) {
        this.submitHarnessTaskCase = submitHarnessTaskCase;
    }

    /**
     * 提交一个任务。
     * <p>
     * 执行流程：
     * 1. 读取请求体；
     * 2. 调用任务提交用例执行；
     * 3. 返回提交响应。
     */
    @Override
    public SubmitTaskResponse submitTask(SubmitTaskRequest request) {
        return submitHarnessTaskCase.execute(request);
    }
}
