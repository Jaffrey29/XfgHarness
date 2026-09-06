package cn.xiaofuge.deepseek.harness.api.service;

import cn.xiaofuge.deepseek.harness.api.dto.task.SubmitTaskRequest;
import cn.xiaofuge.deepseek.harness.api.dto.task.SubmitTaskResponse;

/**
 * 任务提交 API。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IHarnessTaskApi {

    /**
     * 提交任务以供执行。
     */
    SubmitTaskResponse submitTask(SubmitTaskRequest request);
}
