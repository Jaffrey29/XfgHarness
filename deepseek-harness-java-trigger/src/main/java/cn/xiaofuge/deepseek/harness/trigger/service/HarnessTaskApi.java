package cn.xiaofuge.deepseek.harness.trigger.service;

import cn.xiaofuge.deepseek.harness.api.dto.task.SubmitTaskRequest;
import cn.xiaofuge.deepseek.harness.api.dto.task.SubmitTaskResponse;
import cn.xiaofuge.deepseek.harness.cases.task.ISubmitHarnessTaskCase;
import cn.xiaofuge.deepseek.harness.cases.task.SubmitTaskCommand;
import cn.xiaofuge.deepseek.harness.cases.task.SubmitTaskResult;
import cn.xiaofuge.deepseek.harness.api.service.IHarnessTaskApi;
import org.springframework.stereotype.Service;

/**
 * 任务提交适配器。
 * <p>
 * 执行流程：
 * 1. 接收 Trigger 层提交的任务请求 DTO（SubmitTaskRequest）；
 * 2. 映射为 case 层 Command（SubmitTaskCommand），调用任务提交用例；
 * 3. 将用例返回的 Result（SubmitTaskResult）映射回 API DTO（SubmitTaskResponse）。
 * <p>
 * 这一层是 DTO ↔ Command 的边界映射点，case 层不直接依赖 API DTO。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class HarnessTaskApi implements IHarnessTaskApi {

    private final ISubmitHarnessTaskCase submitHarnessTaskCase;

    /**
     * 处理Harness 任务 API。
     * 流程：转换协议 → 调用应用门面 → 返回结果。
     * @param submitHarnessTaskCase submit Harness 任务 Case参数。
     * @return 方法执行结果。
     */
    public HarnessTaskApi(ISubmitHarnessTaskCase submitHarnessTaskCase) {
        this.submitHarnessTaskCase = submitHarnessTaskCase;
    }

    @Override
    public SubmitTaskResponse submitTask(SubmitTaskRequest request) {
        SubmitTaskCommand command = toCommand(request);
        SubmitTaskResult result = submitHarnessTaskCase.execute(command);
        return toResponse(result);
    }

    // ── DTO ↔ Command 映射 ───────────────────────────────────────

    /**
     * 转换为 命令。
     * 流程：转换协议 → 调用应用门面 → 返回结果。
     * @param request 请求对象。
     * @return 转换后的目标对象。
     */
    private SubmitTaskCommand toCommand(SubmitTaskRequest request) {
        return new SubmitTaskCommand(
                request.workspaceId(),
                request.prompt(),
                request.profileCode(),
                request.requestedTools()
        );
    }

    /**
     * 转换为 响应。
     * 流程：转换协议 → 调用应用门面 → 返回结果。
     * @param result 结果对象。
     * @return 转换后的目标对象。
     */
    private SubmitTaskResponse toResponse(SubmitTaskResult result) {
        return new SubmitTaskResponse(
                result.taskId(),
                result.sessionId(),
                result.status(),
                result.resolvedModel(),
                result.enabledTools(),
                result.approvalRequired(),
                result.approvalReason(),
                result.acceptedAt()
        );
    }
}
