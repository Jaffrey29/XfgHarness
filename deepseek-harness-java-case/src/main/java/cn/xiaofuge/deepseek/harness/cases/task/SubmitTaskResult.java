package cn.xiaofuge.deepseek.harness.cases.task;

import java.time.Instant;
import java.util.List;

/**
 * 任务提交用例结果。
 * <p>
 * 用例层的输出对象，与 API 层 DTO 解耦。
 * trigger 层负责将本结果映射为 API 层的 SubmitTaskResponse。
 *
 * @param taskId 生成的任务 ID
 * @param sessionId 生成的会话 ID
 * @param status 当前生命周期状态
 * @param resolvedModel 解析后的模型 ID
 * @param enabledTools 启用的工具列表
 * @param approvalRequired 执行前是否需要人工审批
 * @param approvalReason 审批决策的简要说明
 * @param acceptedAt 接收时间
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record SubmitTaskResult(
        String taskId,
        String sessionId,
        String status,
        String resolvedModel,
        List<String> enabledTools,
        boolean approvalRequired,
        String approvalReason,
        Instant acceptedAt
) {
}
