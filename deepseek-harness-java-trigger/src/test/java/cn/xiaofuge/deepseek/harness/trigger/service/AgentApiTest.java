package cn.xiaofuge.deepseek.harness.trigger.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cn.xiaofuge.deepseek.harness.api.dto.AgentMessageRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.AgentMessageResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.AgentToolCallEventResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.agent.IAgentUseCase;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class AgentApiTest {

    @Test
    void shouldPreserveToolResultPhase() {
        AtomicReference<AgentToolCallEventResponseDTO> sinkEvent = new AtomicReference<>();
        IAgentUseCase useCase = new IAgentUseCase() {
            @Override public AgentMessageResponseDTO sendMessage(AgentMessageRequestDTO request) { return null; }
            @Override public AgentMessageResponseDTO sendMessageStreaming(
                    AgentMessageRequestDTO request,
                    java.util.function.Consumer<String> deltaSink,
                    java.util.function.Consumer<String> reasoningSink,
                    java.util.function.Consumer<AgentToolCallEventResponseDTO> toolCallSink,
                    Runnable finishSink
            ) {
                toolCallSink.accept(new AgentToolCallEventResponseDTO(
                        "call-1", "fs_read", "{}", "SUCCEEDED", "result", "content", 12L
                ));
                return new AgentMessageResponseDTO("agent-1", "session-1", "idle", List.of());
            }
            @Override public String getAgentStatus(String agentId) { return "idle"; }
            @Override public void cancelAgent(String agentId) {}
        };
        AgentApi agentApi = new AgentApi(useCase);

        agentApi.sendMessageStreaming(
                new AgentMessageRequestDTO("agent-1", null, null, null, "message", null, null, null),
                delta -> {}, delta -> {}, sinkEvent::set, () -> {}
        );

        assertEquals("result", sinkEvent.get().phase());
        assertEquals("SUCCEEDED", sinkEvent.get().status());
        assertEquals("content", sinkEvent.get().result());
        assertEquals(12L, sinkEvent.get().durationMs());
    }
}
