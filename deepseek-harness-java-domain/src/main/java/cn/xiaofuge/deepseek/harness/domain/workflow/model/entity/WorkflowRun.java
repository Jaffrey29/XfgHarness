package cn.xiaofuge.deepseek.harness.domain.workflow.model.entity;

import cn.xiaofuge.deepseek.harness.domain.workflow.model.valobj.WorkflowMeta;
import cn.xiaofuge.deepseek.harness.domain.workflow.model.valobj.WorkflowResult;
import java.util.concurrent.CompletableFuture;

/**
 * 工作流运行句柄。
 * <p>
 * 执行流程：
 * 1. 工作流启动后创建运行句柄；
 * 2. 运行过程中可查询结果或发起取消；
 * 3. 运行结束后由句柄负责完成收敛和清理。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class WorkflowRun {

    private final String id;
    private final WorkflowMeta meta;
    private final CompletableFuture<WorkflowResult> result;
    private volatile boolean cancelled = false;

    /**
     * 处理Workflow Run相关逻辑。
     */
    public WorkflowRun(String id, WorkflowMeta meta, CompletableFuture<WorkflowResult> result) {
        this.id = id;
        this.meta = meta;
        this.result = result;
    }

    /**
     * 处理id相关逻辑。
     */
    public String id() { return id; }
    /**
     * 处理meta相关逻辑。
     */
    public WorkflowMeta meta() { return meta; }
    /**
     * 处理result相关逻辑。
     */
    public CompletableFuture<WorkflowResult> result() { return result; }

    /**
     * 取消工作流运行。
     * <p>
     * 执行流程：
     * 1. 标记运行已取消；
     * 2. 如果结果尚未完成则补一个取消结果；
     * 3. 交由上层等待结果收敛。
     */
    public void cancel(String reason) {
        cancelled = true;
        if (!result.isDone()) {
            result.complete(WorkflowResult.cancelled(0));
        }
    }

    /**
     * 释放工作流运行资源。
     * <p>
     * 执行流程：
     * 1. 先触发取消；
     * 2. 等待结果 future 收敛；
     * 3. 返回无返回值的完成信号。
     */
    public CompletableFuture<Void> dispose() {
        cancel(null);
        return result.thenRun(() -> {});
    }

    /**
     * 判断Cancelled是否成立。
     */
    public boolean isCancelled() { return cancelled; }
}
