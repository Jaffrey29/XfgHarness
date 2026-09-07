package cn.xiaofuge.deepseek.harness.cases.agent;

import java.util.List;
import java.util.Map;

/**
 * Agent 消息发送用例结果。
 * <p>
 * 用例层的输出对象，与 API 层 DTO 解耦。
 * trigger 层负责将本结果映射为 API 层的 AgentMessageResponse。
 *
 * @param agentId Agent ID
 * @param sessionId 会话 ID
 * @param status Agent 状态，例如 idle、running
 * @param messages 会话消息
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record SendMessageResult(
        String agentId,
        String sessionId,
        String status,
        List<Map<String, Object>> messages
) {
}
