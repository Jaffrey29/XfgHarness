package cn.xiaofuge.deepseek.harness.cases.task;

import java.util.List;

/**
 * 任务提交用例命令。
 * <p>
 * 用例层的输入对象，与 API 层 DTO 解耦。
 * trigger 层负责将 API 层的 SubmitTaskRequest 映射为本命令。
 *
 * @param workspaceId 调用方工作区 ID
 * @param prompt 用户提示词
 * @param profileCode 执行档位编码
 * @param requestedTools 请求启用的工具编码
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record SubmitTaskCommand(
        String workspaceId,
        String prompt,
        String profileCode,
        List<String> requestedTools
) {
}
