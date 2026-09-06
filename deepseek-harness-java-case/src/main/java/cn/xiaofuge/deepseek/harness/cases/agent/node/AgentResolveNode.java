package cn.xiaofuge.deepseek.harness.cases.agent.node;

import cn.xiaofuge.deepseek.harness.api.dto.agent.AgentMessageRequest;
import cn.xiaofuge.deepseek.harness.api.dto.agent.AgentMessageResponse;
import cn.xiaofuge.deepseek.harness.cases.agent.factory.AgentMessageDynamicContext;
import cn.xiaofuge.deepseek.harness.domain.agent.model.entity.AgentOptions;
import cn.xiaofuge.deepseek.harness.domain.agent.service.AgentFactoryPort;
import cn.xiaofuge.deepseek.harness.domain.agent.service.ReactLoopAgent;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.IModelSettingQueryService;
import cn.xiaofuge.deepseek.harness.domain.shared.StrategyHandler;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 解析节点：获取或创建目标 Agent。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service("agentMessageResolveNode")
public class AgentResolveNode extends AbstractAgentMessageNode {

    private static final Logger log = LoggerFactory.getLogger(AgentResolveNode.class);

    @Autowired
    private AgentFactoryPort agentFactory;

    @Autowired
    private IModelSettingQueryService modelSettingQueryService;

    @Autowired
    @Qualifier("agentMessageIntentNode")
    private AgentIntentNode intentNode;

    @Value("${harness.llm.deepseek.default-model:deepseek}")
    private String defaultModel;

    @Override
    protected AgentMessageResponse doApply(AgentMessageRequest request,
                                           AgentMessageDynamicContext ctx) throws Exception {
        ReactLoopAgent agent = agentFactory.get(request.agentId());
        if (agent == null) {
            AgentOptions options = buildAgentOptions(request);
            String workDir = request.cwd() != null ? request.cwd() : System.getProperty("user.dir");
            log.info("[Resolve] Agent={} 不存在，创建新实例（model={} provider={} cwd={}）",
                    request.agentId(), options.model(), options.provider(), workDir);
            agent = agentFactory.create(request.agentId(), options, workDir);
        } else {
            log.debug("[Resolve] Agent={} 已存在，复用实例", request.agentId());
        }
        ctx.setAgent(agent);
        return null;
    }

    @Override
    public StrategyHandler<AgentMessageRequest, AgentMessageDynamicContext, AgentMessageResponse>
    getNext(AgentMessageRequest request, AgentMessageDynamicContext ctx) {
        return intentNode;
    }

    private AgentOptions buildAgentOptions(AgentMessageRequest request) {
        String provider = request.provider() != null ? request.provider() : "deepseek";
        String model = request.model() != null ? request.model() : defaultModel;
        String baseUrl = null;
        String apiKey = null;
        var setting = modelSettingQueryService.queryAll().stream()
                .filter(s -> s.enabled())
                .filter(s -> s.modelCode() != null && s.modelCode().equals(model))
                .findFirst();
        if (setting.isPresent()) {
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
