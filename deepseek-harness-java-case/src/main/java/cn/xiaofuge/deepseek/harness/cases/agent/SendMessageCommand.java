package cn.xiaofuge.deepseek.harness.cases.agent;

import java.util.List;

/**
 * Agent 消息发送用例命令。
 * <p>
 * 用例层的输入对象，与 API 层 DTO 解耦。
 * trigger 层负责将 API 层的 AgentMessageRequest 映射为本命令。
 *
 * @param agentId Agent ID
 * @param provider 模型提供方；为空时使用默认值
 * @param model 模型名称；为空时使用默认值
 * @param maxTokens 最大输出 Token 数；为空时使用默认值
 * @param cwd 工作目录；为空时使用进程工作目录
 * @param message 用户消息文本
 * @param images 图片附件列表；每项为 data URL 或裸 base64，可为 null/空
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record SendMessageCommand(
        String agentId,
        String provider,
        String model,
        Integer maxTokens,
        String cwd,
        String message,
        List<String> images
) {
    public SendMessageCommand {
        if (agentId == null || agentId.isBlank()) {
            throw new IllegalArgumentException("agentId must be non-blank");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must be non-blank");
        }
    }
}
