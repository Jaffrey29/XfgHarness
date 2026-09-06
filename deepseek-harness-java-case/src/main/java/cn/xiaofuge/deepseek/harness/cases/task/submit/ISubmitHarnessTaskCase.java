package cn.xiaofuge.deepseek.harness.cases.task.submit;

import cn.xiaofuge.deepseek.harness.api.dto.task.SubmitTaskRequest;
import cn.xiaofuge.deepseek.harness.api.dto.task.SubmitTaskResponse;

/**
 * 编排任务提交用例。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface ISubmitHarnessTaskCase {

    /**
     * 执行任务提交流程。
     *
     * @param request 任务请求
     * @return 已接收的任务摘要
     */
    SubmitTaskResponse execute(SubmitTaskRequest request);
}
