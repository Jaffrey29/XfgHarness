package cn.walioffice.deepseek.harness.api.dto.agent;

/**
 * 向 Agent 发送消息的请求。
 *
 * @param agentId Agent ID
 * @param provider 模型提供方；为空时使用默认值
 * @param model 模型名称；为空时使用默认值
 * @param maxTokens 最大输出 Token 数；为空时使用默认值
 * @param cwd 工作目录；为空时使用进程工作目录
 * @param message 用户消息文本
 * @param images 图片附件列表；每项为 data URL（data:image/png;base64,...）或裸 base64，
 *               可为 null/空。仅多模态模型支持，文本-only 模型会忽略或报错。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record AgentMessageRequest(
        String agentId,
        String provider,
        String model,
        Integer maxTokens,
        String cwd,
        String message,
        java.util.List<String> images
) {
    public AgentMessageRequest {
        if (agentId == null || agentId.isBlank()) {
            throw new IllegalArgumentException("agentId must be non-blank");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must be non-blank");
        }
    }
}
