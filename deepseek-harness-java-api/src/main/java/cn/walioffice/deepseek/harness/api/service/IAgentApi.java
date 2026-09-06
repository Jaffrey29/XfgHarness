package cn.walioffice.deepseek.harness.api.service;

import cn.walioffice.deepseek.harness.api.dto.agent.AgentMessageRequest;
import cn.walioffice.deepseek.harness.api.dto.agent.AgentMessageResponse;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.ToolCallBlock;

/**
 * Agent 交互 API。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IAgentApi {

    /**
     * 向 Agent 发送消息；如果 Agent 不存在则先创建。
     */
    AgentMessageResponse sendMessage(AgentMessageRequest request);

    /**
     * 使用实时增量接收器发送消息。助手文本每产生一个增量，都会立即推送到 sink，
     * 调用方（例如 SSE Controller）无需等待完整回合结束就能向前端推送分片。
     */
    AgentMessageResponse sendMessageStreaming(
            AgentMessageRequest request,
            java.util.function.Consumer<String> deltaSink,
            java.util.function.Consumer<ToolCallBlock> toolCallSink);

    /**
     * 查询 Agent 状态。
     */
    String getAgentStatus(String agentId);

    /**
     * 取消 Agent 当前活动。
     */
    void cancelAgent(String agentId);
}
