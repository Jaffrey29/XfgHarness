package cn.xiaofuge.deepseek.harness.api.dto.agent;

import java.util.List;
import java.util.Map;

/**
 * Agent 消息发送响应。
 *
 * @param agentId Agent ID
 * @param sessionId 会话 ID
 * @param status Agent 状态，例如 idle、running
 * @param messages 会话消息
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record AgentMessageResponse(
        String agentId,
        String sessionId,
        String status,
        List<Map<String, Object>> messages
) {
}
