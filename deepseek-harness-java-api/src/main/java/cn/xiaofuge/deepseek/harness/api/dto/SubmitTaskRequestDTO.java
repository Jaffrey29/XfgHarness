package cn.xiaofuge.deepseek.harness.api.dto;

import java.util.List;

/**
 * 任务提交请求。
 *
 * @param workspaceId 调用方工作区 ID
 * @param prompt 用户提示词
 * @param profileCode 执行档位编码
 * @param requestedTools 请求启用的工具编码
 *
 * <p>调用流程：Controller/DTO → 应用门面 → 用例；响应对象由用例结果转换。</p>
 * <p>示例：DTO 只做协议隔离，不承载领域规则。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public record SubmitTaskRequestDTO(
        String workspaceId,
        String prompt,
        String profileCode,
        List<String> requestedTools
) {
}
