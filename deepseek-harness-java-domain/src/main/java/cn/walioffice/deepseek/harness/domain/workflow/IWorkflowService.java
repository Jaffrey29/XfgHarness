package cn.walioffice.deepseek.harness.domain.workflow;

import cn.walioffice.deepseek.harness.domain.workflow.model.entity.WorkflowRun;
import cn.walioffice.deepseek.harness.domain.workflow.model.entity.WorkflowStartRequest;
import java.util.concurrent.CompletableFuture;

/**
 * 工作流服务接口。
 * <p>
 * 执行流程：
 * 1. 上层提交工作流启动请求；
 * 2. 领域层交给工作流引擎端口执行；
 * 3. 返回可取消的运行句柄供后续控制。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IWorkflowService {

    /**
     * 启动一个工作流运行。
     * <p>
     * 执行流程：
     * 1. 读取启动请求；
     * 2. 调用工作流引擎创建运行句柄；
     * 3. 返回运行结果的未来对象。
     */
    WorkflowRun start(WorkflowStartRequest request);

    /**
     * 取消正在运行的工作流。
     * <p>
     * 执行流程：
     * 1. 根据 runId 查找运行实例；
     * 2. 将取消原因传给运行实例；
     * 3. 由运行实例完成状态收敛。
     */
    void cancel(String runId, String reason);
}
