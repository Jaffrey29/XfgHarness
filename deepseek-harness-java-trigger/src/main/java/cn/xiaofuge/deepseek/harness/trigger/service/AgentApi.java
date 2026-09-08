package cn.xiaofuge.deepseek.harness.trigger.service;

import cn.xiaofuge.deepseek.harness.cases.agent.IAgentUseCase;
import cn.xiaofuge.deepseek.harness.api.IAgentApi;
import cn.xiaofuge.deepseek.harness.api.dto.AgentMessageRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.AgentMessageResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.AgentToolCallEventResponseDTO;
import cn.xiaofuge.deepseek.harness.api.response.Response;
import org.springframework.stereotype.Service;

/**
 * 智能体应用适配器。
 * <p>
 * 执行流程：
 * 1. 接收触发层传入的 API DTO；
 * 2. 调用 Case 层用例；
 * 3. 返回 API DTO。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class AgentApi implements IAgentApi {

    private final IAgentUseCase agentUseCase;

    /**
     * 处理Agent API。
     * 流程：转换协议 → 调用应用门面 → 返回结果。
     * @param agentUseCase Agent Use Case参数。
     * @return 方法执行结果。
     */
    public AgentApi(IAgentUseCase agentUseCase) {
        this.agentUseCase = agentUseCase;
    }

    /**
     * 获取Agent Status。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param agentId Agent ID。
     * @return 获取到的Agent Status。
     */

    /**
     * 取消Agent。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param agentId Agent ID。
     */

    @Override
    public Response<AgentMessageResponseDTO> sendMessage(AgentMessageRequestDTO request) {
        return execute(() -> agentUseCase.sendMessage(request));
    }

    /**
     * 取消Agent。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param agentId Agent ID。
     */

    @Override
    public Response<AgentMessageResponseDTO> sendMessageStreaming(
            AgentMessageRequestDTO request,
            java.util.function.Consumer<String> deltaSink,
            java.util.function.Consumer<String> reasoningSink,
            java.util.function.Consumer<AgentToolCallEventResponseDTO> toolCallSink,
            Runnable finishSink) {
        return execute(() -> agentUseCase.sendMessageStreaming(
                    request,
                    deltaSink,
                    reasoningSink,
                    event -> toolCallSink.accept(new AgentToolCallEventResponseDTO(
                            event.callId(),
                            event.toolName(),
                            event.arguments(),
                            "running"
                    )),
                    finishSink
        ));
    }

    @Override
    public Response<String> getAgentStatus(String agentId) {
        return execute(() -> agentUseCase.getAgentStatus(agentId));
    }
    /**
     * 取消Agent。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param agentId Agent ID。
     */


    @Override
    public Response<Void> cancelAgent(String agentId) {
        return executeAction(() -> agentUseCase.cancelAgent(agentId));
    }

    private <T> Response<T> execute(java.util.function.Supplier<T> action) {
        try {
            return Response.success(action.get());
        } catch (IllegalArgumentException exception) {
            return Response.invalidArgument(exception.getMessage());
        } catch (RuntimeException exception) {
            return Response.internalError(exception.getMessage());
        }
    }

    private Response<Void> executeAction(Runnable action) {
        try {
            action.run();
            return Response.success(null);
        } catch (IllegalArgumentException exception) {
            return Response.invalidArgument(exception.getMessage());
        } catch (RuntimeException exception) {
            return Response.internalError(exception.getMessage());
        }
    }

    // ── DTO ↔ Command 映射 ───────────────────────────────────────

    /**
     * 转换为 命令。
     * 流程：转换协议 → 调用应用门面 → 返回结果。
     * @param request 请求对象。
     * @return 转换后的目标对象。
     */
}
