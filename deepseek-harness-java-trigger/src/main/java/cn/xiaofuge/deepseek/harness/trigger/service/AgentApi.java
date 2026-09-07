package cn.xiaofuge.deepseek.harness.trigger.service;

import cn.xiaofuge.deepseek.harness.api.dto.agent.AgentMessageRequest;
import cn.xiaofuge.deepseek.harness.api.dto.agent.AgentMessageResponse;
import cn.xiaofuge.deepseek.harness.cases.agent.IAgentUseCase;
import cn.xiaofuge.deepseek.harness.cases.agent.SendMessageCommand;
import cn.xiaofuge.deepseek.harness.cases.agent.SendMessageResult;
import cn.xiaofuge.deepseek.harness.api.service.IAgentApi;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.ToolCallBlock;
import org.springframework.stereotype.Service;

/**
 * 智能体应用适配器。
 * <p>
 * 执行流程：
 * 1. 接收触发层传入的 API DTO（AgentMessageRequest）；
 * 2. 映射为 case 层 Command（SendMessageCommand），调用用例层；
 * 3. 将用例层返回的 Result（SendMessageResult）映射回 API DTO（AgentMessageResponse）。
 * <p>
 * 这一层是 DTO ↔ Command 的边界映射点，case 层不直接依赖 API DTO。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class AgentApi implements IAgentApi {

    private final IAgentUseCase agentUseCase;

    /**
     * 处理Agent Api相关逻辑。
     */
    public AgentApi(IAgentUseCase agentUseCase) {
        this.agentUseCase = agentUseCase;
    }

    @Override
    public AgentMessageResponse sendMessage(AgentMessageRequest request) {
        SendMessageCommand command = toCommand(request);
        SendMessageResult result = agentUseCase.sendMessage(command);
        return toResponse(result);
    }

    @Override
    public AgentMessageResponse sendMessageStreaming(
            AgentMessageRequest request,
            java.util.function.Consumer<String> deltaSink,
            java.util.function.Consumer<ToolCallBlock> toolCallSink) {
        SendMessageCommand command = toCommand(request);
        SendMessageResult result = agentUseCase.sendMessageStreaming(command, deltaSink, toolCallSink);
        return toResponse(result);
    }

    @Override
    public String getAgentStatus(String agentId) {
        return agentUseCase.getAgentStatus(agentId);
    }

    @Override
    public void cancelAgent(String agentId) {
        agentUseCase.cancelAgent(agentId);
    }

    // ── DTO ↔ Command 映射 ───────────────────────────────────────

    /**
     * 处理to Command相关逻辑。
     */
    private SendMessageCommand toCommand(AgentMessageRequest request) {
        return new SendMessageCommand(
                request.agentId(),
                request.provider(),
                request.model(),
                request.maxTokens(),
                request.cwd(),
                request.message(),
                request.images()
        );
    }

    /**
     * 处理to Response相关逻辑。
     */
    private AgentMessageResponse toResponse(SendMessageResult result) {
        return new AgentMessageResponse(
                result.agentId(),
                result.sessionId(),
                result.status(),
                result.messages()
        );
    }
}
