package cn.xiaofuge.deepseek.harness.api;

import cn.xiaofuge.deepseek.harness.api.dto.AgentMessageRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.AgentMessageResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.AgentToolCallEventResponseDTO;
import cn.xiaofuge.deepseek.harness.api.response.Response;

public interface IAgentApi {
    Response<AgentMessageResponseDTO> sendMessage(AgentMessageRequestDTO request);

    Response<AgentMessageResponseDTO> sendMessageStreaming(
            AgentMessageRequestDTO request,
            java.util.function.Consumer<String> deltaSink,
            java.util.function.Consumer<String> reasoningSink,
            java.util.function.Consumer<AgentToolCallEventResponseDTO> toolCallSink);

    Response<String> getAgentStatus(String agentId);

    Response<Void> cancelAgent(String agentId);
}
