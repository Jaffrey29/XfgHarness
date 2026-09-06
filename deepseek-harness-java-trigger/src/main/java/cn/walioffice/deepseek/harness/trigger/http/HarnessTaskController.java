package cn.walioffice.deepseek.harness.trigger.http;

import cn.walioffice.deepseek.harness.api.dto.task.SubmitTaskRequest;
import cn.walioffice.deepseek.harness.api.dto.task.SubmitTaskResponse;
import cn.walioffice.deepseek.harness.api.service.IHarnessTaskApi;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 任务提交控制器。
 * <p>
 * 执行流程：
 * 1. 接收前端提交任务请求；
 * 2. 交给任务用例完成校验、排队和状态落库；
 * 3. 返回任务提交结果。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@RestController
@RequestMapping("/api/harness/tasks")
public class HarnessTaskController {

    private final IHarnessTaskApi harnessTaskApi;

    public HarnessTaskController(IHarnessTaskApi harnessTaskApi) {
        this.harnessTaskApi = harnessTaskApi;
    }

    /**
     * 提交一个任务。
     * <p>
     * 执行流程：
     * 1. 读取请求体；
     * 2. 调用任务 API 执行提交；
     * 3. 返回提交结果给前端。
     */
    @PostMapping("/submit")
    public SubmitTaskResponse submitTask(@RequestBody SubmitTaskRequest request) {
        return harnessTaskApi.submitTask(request);
    }
}
