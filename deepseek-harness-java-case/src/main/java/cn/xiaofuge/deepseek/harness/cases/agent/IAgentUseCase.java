package cn.xiaofuge.deepseek.harness.cases.agent;

import cn.xiaofuge.deepseek.harness.api.dto.agent.AgentMessageRequest;
import cn.xiaofuge.deepseek.harness.api.dto.agent.AgentMessageResponse;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.ToolCallBlock;

/**
 * Agent 交互用例。
 * <p>
 * 用例位于 Case 层；API 层负责 DTO 转换后调用本接口。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IAgentUseCase {

    /**
     * 向 Agent 发送消息；如果 Agent 不存在则先创建。
     *
     * @param request Agent 消息请求
     * @return Agent 消息响应
     */
    AgentMessageResponse sendMessage(AgentMessageRequest request);

    /**
     * 使用实时增量接收器发送消息，用于流式输出。
     */
    AgentMessageResponse sendMessageStreaming(
            AgentMessageRequest request,
            java.util.function.Consumer<String> deltaSink,
            java.util.function.Consumer<ToolCallBlock> toolCallSink);

    /**
     * 查询 Agent 当前状态。
     *
     * @param agentId Agent ID
     * @return 状态字符串，例如 idle、running、not-found
     */
    String getAgentStatus(String agentId);

    /**
     * 取消正在运行的 Agent。
     *
     * @param agentId Agent ID
     */
    void cancelAgent(String agentId);
}
