package cn.xiaofuge.deepseek.harness.cases.agent;

import cn.xiaofuge.deepseek.harness.api.dto.agent.AgentMessageRequest;
import cn.xiaofuge.deepseek.harness.api.dto.agent.AgentMessageResponse;
import cn.xiaofuge.deepseek.harness.cases.agent.factory.AgentMessageFactory;
import cn.xiaofuge.deepseek.harness.domain.agent.model.valobj.AgentCancelCause;
import cn.xiaofuge.deepseek.harness.domain.agent.model.entity.AgentOptions;
import cn.xiaofuge.deepseek.harness.domain.agent.service.AgentFactoryPort;
import cn.xiaofuge.deepseek.harness.domain.agent.service.ReactLoopAgent;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.ToolCallBlock;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.IModelSettingQueryService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Agent 交互用例。
 * 消息发送通过工厂委托给策略树处理。
 *
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
    private IModelSettingQueryService modelSettingQueryService;

    @org.springframework.beans.factory.annotation.Value("${harness.llm.deepseek.default-model:gpt-5.5}")
    private String defaultModel;

    @Override
    public AgentMessageResponse sendMessage(AgentMessageRequest request) {
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

    @Override
    public AgentMessageResponse sendMessageStreaming(
            AgentMessageRequest request,
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

    @Override
    public String getAgentStatus(String agentId) {
        ReactLoopAgent agent = agentFactory.get(agentId);
        if (agent == null) return "not-found";
        return agent.status().name().toLowerCase();
    }

    @Override
    public void cancelAgent(String agentId) {
        ReactLoopAgent agent = agentFactory.get(agentId);
        if (agent != null) {
            agent.cancel(new AgentCancelCause.UserCancel("user requested"), false);
        }
    }

    private AgentOptions buildAgentOptions(AgentMessageRequest request) {
        String provider = request.provider() != null ? request.provider() : "deepseek";
        // 与 AgentResolveNode 保持一致：默认模型用 yml 的 harness.llm.deepseek.default-model（gpt-5.5），
        // 而不是写死的 "deepseek"（库里没有该 modelCode，会匹配不到 → apiKey=null → Missing API key）。
        String model = request.model() != null ? request.model() : defaultModel;
        String baseUrl = null;
        String apiKey = null;
        var setting = modelSettingQueryService.queryAll().stream()
                .filter(s -> s.enabled())
                .filter(s -> s.modelCode() != null && s.modelCode().equals(model))
                .findFirst();
        if (setting.isPresent()) {
            provider = setting.get().providerCode();
            baseUrl = setting.get().baseUrl();
            apiKey = setting.get().apiKeyRef();
        }
        return new AgentOptions(
                provider,
                model,
                request.maxTokens() != null ? request.maxTokens() : 8192,
                baseUrl,
                apiKey
        );
    }
}
