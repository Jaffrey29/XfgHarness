package cn.xiaofuge.deepseek.harness.api.service;

import cn.xiaofuge.deepseek.harness.api.dto.task.SubmitTaskRequest;
import cn.xiaofuge.deepseek.harness.api.dto.task.SubmitTaskResponse;

/**
 * 任务提交 API。
 *
 * <p>调用流程：Controller/DTO → 应用门面 → 用例；响应对象由用例结果转换。</p>
 * <p>示例：DTO 只做协议隔离，不承载领域规则。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IHarnessTaskApi {

    /**
     * 提交任务以供执行。
     */
    SubmitTaskResponse submitTask(SubmitTaskRequest request);
}
