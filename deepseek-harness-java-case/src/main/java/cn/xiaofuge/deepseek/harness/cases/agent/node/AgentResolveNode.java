package cn.xiaofuge.deepseek.harness.cases.agent.node;

import cn.xiaofuge.deepseek.harness.api.dto.AgentMessageRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.AgentMessageResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.agent.factory.AgentMessageDynamicContext;
import cn.xiaofuge.deepseek.harness.domain.agent.model.entity.AgentOptions;
import cn.xiaofuge.deepseek.harness.domain.agentrun.AgentRun;
import cn.xiaofuge.deepseek.harness.domain.agentrun.AgentRunFactoryPort;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.service.AgentModelSettingResolver;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 解析节点：获取或创建目标 Agent。
 *
 * <p>调用流程：HTTP/API 门面 → AgentUseCase → AgentMessageFactory → 策略树 → ReactLoopAgent；会话事件写入 SessionEventLog。</p>
 * <p>示例：调用方传入 agentId、用户消息和可选模型配置，返回助手文本与工具调用结果。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service("agentMessageResolveNode")
public class AgentResolveNode extends AbstractAgentMessageNode {

    private static final Logger log = LoggerFactory.getLogger(AgentResolveNode.class);

    @Autowired
    private AgentRunFactoryPort agentFactory;

    @Autowired
    private AgentModelSettingResolver modelSettingResolver;

    @Autowired
    @Qualifier("agentMessageIntentNode")
    private AgentIntentNode intentNode;

    /**
     * 解析或创建本次消息要使用的 Agent。
     * <p>执行流程：按 agentId 查缓存；命中则复用，未命中则用默认 provider/model 和工作目录创建。
     * 解析结果只写入上下文，本节点不产生最终响应。
     * <p>数据示例：agentId="a-1"，request.model="deepseek-chat"，cwd=null，则使用当前进程目录创建 Agent。
     */
    @Override
    protected AgentMessageResponseDTO doApply(AgentMessageRequestDTO request,
                                           AgentMessageDynamicContext ctx) throws Exception {
        AgentRun agent = agentFactory.get(request.agentId());
        if (agent == null) {
            AgentOptions options = buildAgentOptions(request);
            String workDir = request.cwd() != null ? request.cwd() : System.getProperty("user.dir");
            log.info("[Resolve] Agent={} 不存在，创建新实例（model={} provider={} cwd={}）",
                    request.agentId(), options.model(), options.provider(), workDir);
            agent = agentFactory.create(request.agentId(), options, workDir);
        } else {
            log.debug("[Resolve] Agent={} 已存在，复用实例", request.agentId());
            agent.updateOptions(buildAgentOptions(request));
        }
        ctx.attachSinks(agent);
        ctx.setAgent(agent);
        return null;
    }

    @Override
    public StrategyHandler<AgentMessageRequestDTO, AgentMessageDynamicContext, AgentMessageResponseDTO>
    /**
     * 获取Next。
     * 流程：读取命令 → 执行用例编排 → 返回结果。
     * @param request 请求对象。
     * @param ctx ctx参数。
     * @return 获取到的Next。
     */

    getNext(AgentMessageRequestDTO request, AgentMessageDynamicContext ctx) {
        return intentNode;
    }

    /**
     * 将 HTTP 请求字段转换为 Agent 运行参数。
     * <p>统一使用 {@link AgentModelOptionsResolver}：请求显式值优先，其次数据库默认配置，
     * 最后才使用 application.yml 的模型兜底。
     */
    private AgentOptions buildAgentOptions(AgentMessageRequestDTO request) {
        return modelSettingResolver.resolve(request.provider(), request.model(), request.maxTokens());
    }
}
