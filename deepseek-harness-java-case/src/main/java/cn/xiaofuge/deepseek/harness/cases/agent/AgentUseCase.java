package cn.xiaofuge.deepseek.harness.cases.agent;

import cn.xiaofuge.deepseek.harness.cases.agent.SendMessageCommand;
import cn.xiaofuge.deepseek.harness.cases.agent.SendMessageResult;
import cn.xiaofuge.deepseek.harness.cases.agent.factory.AgentMessageFactory;
import cn.xiaofuge.deepseek.harness.domain.agent.model.valobj.AgentCancelCause;
import cn.xiaofuge.deepseek.harness.domain.agent.model.entity.AgentOptions;
import cn.xiaofuge.deepseek.harness.domain.agent.service.AgentFactoryPort;
import cn.xiaofuge.deepseek.harness.domain.agent.service.ReactLoopAgent;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.ToolCallBlock;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Agent 交互用例。
 * 消息发送通过工厂委托给策略树处理。
 *
 * <p>调用流程：HTTP/API 门面 → AgentUseCase → AgentMessageFactory → 策略树 → ReactLoopAgent；会话事件写入 SessionEventLog。</p>
 * <p>示例：调用方传入 agentId、用户消息和可选模型配置，返回助手文本与工具调用结果。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class AgentUseCase implements IAgentUseCase {

    private static final Logger log = LoggerFactory.getLogger(AgentUseCase.class);

    @Autowired
    private AgentMessageFactory agentMessageFactory;

    @Autowired
    private AgentFactoryPort agentFactory;

    @Autowired
    private AgentModelOptionsResolver modelOptionsResolver;

    /**
     * 发送Message。
     * 流程：读取命令 → 执行用例编排 → 返回结果。
     * @param request 请求对象。
     * @return 方法执行结果。
     */

    @Override
    public SendMessageResult sendMessage(SendMessageCommand request) {
        log.info("[UseCase] Agent={} 收到消息发送请求（provider={} model={} streaming=false）",
                request.agentId(), request.provider(), request.model());
        try {
            return agentMessageFactory.strategyHandler()
                    .apply(request, agentMessageFactory.newContext());
        } catch (RuntimeException e) {
            log.error("[UseCase] Agent={} 消息发送失败（RuntimeException）：{}", request.agentId(), e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("[UseCase] Agent={} 消息发送失败：{}", request.agentId(), e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 发送Message Streaming。
     * 流程：读取命令 → 执行用例编排 → 返回结果。
     * @param request 请求对象。
     * @param deltaSink 文本增量接收器。
     * @param toolCallSink 工具调用事件接收器。
     * @return 方法执行结果。
     */

    @Override
    public SendMessageResult sendMessageStreaming(
            SendMessageCommand request,
            java.util.function.Consumer<String> deltaSink,
            java.util.function.Consumer<ToolCallBlock> toolCallSink) {
        log.info("[UseCase] Agent={} 收到流式消息发送请求（provider={} model={}）",
                request.agentId(), request.provider(), request.model());
        // 先解析或创建 Agent，以便回合开始前挂载增量接收器。
        // 策略处理器会复用这里的 Agent 实例。
        ReactLoopAgent agent = agentFactory.get(request.agentId());
        if (agent == null) {
            AgentOptions options = buildAgentOptions(request);
            String workDir = request.cwd() != null ? request.cwd() : System.getProperty("user.dir");
            log.info("[UseCase] Agent={} 不存在，流式模式创建新实例（model={}）", request.agentId(), options.model());
            agent = agentFactory.create(request.agentId(), options, workDir);
        }
        if (request.provider() != null || request.model() != null) {
            log.info("[UseCase] Agent={} 请求指定 provider/model，更新 options", request.agentId());
            agent.updateOptions(buildAgentOptions(request));
        }
        agent.setStreamDeltaSink(deltaSink);
        // 工具调用事件接收器：直接交给 controller 的 sink 推送独立 SSE 事件，
        // 让前端正确分段渲染多 step 文本。
        agent.setStreamToolCallSink(toolCallSink);
        try {
            return sendMessage(request);
        } finally {
            agent.setStreamDeltaSink(null);
            agent.setStreamToolCallSink(null);
            log.info("[UseCase] Agent={} 流式请求结束，已清理 sink", request.agentId());
        }
    }

    /**
     * 获取Agent Status。
     * 流程：读取命令 → 执行用例编排 → 返回结果。
     * @param agentId Agent ID。
     * @return 获取到的Agent Status。
     */

    @Override
    public String getAgentStatus(String agentId) {
        ReactLoopAgent agent = agentFactory.get(agentId);
        if (agent == null) return "not-found";
        return agent.status().name().toLowerCase();
    }

    /**
     * 取消Agent。
     * 流程：读取命令 → 执行用例编排 → 返回结果。
     * @param agentId Agent ID。
     */

    @Override
    public void cancelAgent(String agentId) {
        ReactLoopAgent agent = agentFactory.get(agentId);
        if (agent != null) {
            agent.cancel(new AgentCancelCause.UserCancel("user requested"), false);
        }
    }

    /**
     * 构建Agent Options。
     * <p>统一交给 {@link AgentModelOptionsResolver} 处理，避免文件兜底值和数据库设置分散竞争。
     */
    private AgentOptions buildAgentOptions(SendMessageCommand request) {
        return modelOptionsResolver.resolve(request);
    }
}
