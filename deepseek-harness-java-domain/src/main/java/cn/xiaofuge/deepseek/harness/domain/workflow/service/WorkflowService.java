package cn.xiaofuge.deepseek.harness.domain.workflow.service;

import cn.xiaofuge.deepseek.harness.domain.workflow.IWorkflowService;
import cn.xiaofuge.deepseek.harness.domain.workflow.adapter.port.IWorkflowEnginePort;
import cn.xiaofuge.deepseek.harness.domain.workflow.model.entity.WorkflowRun;
import cn.xiaofuge.deepseek.harness.domain.workflow.model.entity.WorkflowStartRequest;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * 工作流服务实现。
 * <p>
 * 执行流程：
 * 1. 启动时交给工作流引擎端口执行；
 * 2. 将运行句柄登记到内存表；
 * 3. 在运行结束后自动移除句柄。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class WorkflowService implements IWorkflowService {

    private final IWorkflowEnginePort enginePort;
    private final ConcurrentHashMap<String, WorkflowRun> liveRuns = new ConcurrentHashMap<>();

    /**
     * 处理Workflow Service相关逻辑。
     */
    public WorkflowService(IWorkflowEnginePort enginePort) {
        this.enginePort = enginePort;
    }

    /**
     * 启动工作流。
     * <p>
     * 执行流程：
     * 1. 将启动请求交给引擎端口；
     * 2. 记录运行句柄；
     * 3. 在结果完成后清理内存状态。
     */
    @Override
    public WorkflowRun start(WorkflowStartRequest request) {
        WorkflowRun run = enginePort.start(request);
        liveRuns.put(run.id(), run);
        run.result().whenComplete((r, e) -> liveRuns.remove(run.id()));
        return run;
    }

    /**
     * 取消工作流。
     * <p>
     * 执行流程：
     * 1. 根据 runId 查询运行句柄；
     * 2. 若存在则发起取消；
     * 3. 由运行句柄完成结果收敛。
     */
    @Override
    public void cancel(String runId, String reason) {
        WorkflowRun run = liveRuns.get(runId);
        if (run != null) run.cancel(reason);
    }
}
