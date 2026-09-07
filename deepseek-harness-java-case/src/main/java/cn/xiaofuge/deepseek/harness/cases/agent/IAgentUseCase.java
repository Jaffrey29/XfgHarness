package cn.xiaofuge.deepseek.harness.cases.agent;

import cn.xiaofuge.deepseek.harness.domain.model.valobj.ToolCallBlock;

/**
 * Agent 交互用例。
 * <p>
 * 用例位于 Case 层，使用 case 层自有 Command/Result 对象，
 * 不直接依赖 API 层 DTO——映射由 trigger 层的 AgentApi 完成。
 *
 * <p>调用流程：HTTP/API 门面 → AgentUseCase → AgentMessageFactory → 策略树 → ReactLoopAgent；会话事件写入 SessionEventLog。</p>
 * <p>示例：调用方传入 agentId、用户消息和可选模型配置，返回助手文本与工具调用结果。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IAgentUseCase {

    /**
     * 向 Agent 发送消息；如果 Agent 不存在则先创建。
     *
     * @param command Agent 消息发送命令
     * @return Agent 消息发送结果
     */
    SendMessageResult sendMessage(SendMessageCommand command);

    /**
     * 使用实时增量接收器发送消息，用于流式输出。
     */
    SendMessageResult sendMessageStreaming(
            SendMessageCommand command,
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
