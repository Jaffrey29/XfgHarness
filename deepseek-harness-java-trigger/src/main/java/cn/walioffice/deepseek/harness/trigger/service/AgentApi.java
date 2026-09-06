package cn.walioffice.deepseek.harness.api.service;

import cn.walioffice.deepseek.harness.api.dto.agent.AgentMessageRequest;
import cn.walioffice.deepseek.harness.api.dto.agent.AgentMessageResponse;
import cn.walioffice.deepseek.harness.cases.agent.IAgentUseCase;
import org.springframework.stereotype.Service;

/**
 * 智能体应用适配器。
 * <p>
 * 执行流程：
 * 1. 接收触发层传入的智能体请求对象；
 * 2. 转交用例层完成消息发送、流式输出、状态查询或取消；
 * 3. 将结果原样返回给触发层。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class AgentApi implements IAgentApi {

    private final IAgentUseCase agentUseCase;

    public AgentApi(IAgentUseCase agentUseCase) {
        this.agentUseCase = agentUseCase;
    }

    /**
     * 发送一条普通智能体消息。
     * <p>
     * 执行流程：
     * 1. 接收请求体；
     * 2. 调用用例层完成一次完整会话处理；
     * 3. 返回消息响应。
     */
    @Override
    public AgentMessageResponse sendMessage(AgentMessageRequest request) {
        return agentUseCase.sendMessage(request);
    }

    /**
     * 以流式方式发送智能体消息。
     * <p>
     * 执行流程：
     * 1. 接收请求体和增量回调；
     * 2. 调用用例层边生成边回调内容；
     * 3. 返回最终消息响应。
     */
    @Override
    public AgentMessageResponse sendMessageStreaming(
            AgentMessageRequest request,
            java.util.function.Consumer<String> deltaSink,
            java.util.function.Consumer<cn.walioffice.deepseek.harness.domain.llm.model.valobj.ToolCallBlock> toolCallSink) {
        return agentUseCase.sendMessageStreaming(request, deltaSink, toolCallSink);
    }

    /**
     * 查询智能体当前状态。
     * <p>
     * 执行流程：
     * 1. 读取 agentId；
     * 2. 调用用例层查询状态；
     * 3. 返回状态字符串。
     */
    @Override
    public String getAgentStatus(String agentId) {
        return agentUseCase.getAgentStatus(agentId);
    }

    /**
     * 取消指定智能体。
     * <p>
     * 执行流程：
     * 1. 读取 agentId；
     * 2. 调用用例层发起取消；
     * 3. 无返回值。
     */
    @Override
    public void cancelAgent(String agentId) {
        agentUseCase.cancelAgent(agentId);
    }
}
