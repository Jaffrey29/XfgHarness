package cn.xiaofuge.deepseek.harness.api.dto.task;

import java.util.List;

/**
 * 任务提交请求。
 *
 * @param workspaceId 调用方工作区 ID
 * @param prompt 用户提示词
 * @param profileCode 执行档位编码
 * @param requestedTools 请求启用的工具编码
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record SubmitTaskRequest(
        String workspaceId,
        String prompt,
        String profileCode,
        List<String> requestedTools
) {
}
