package cn.xiaofuge.deepseek.harness.cases.agent.factory;

import cn.xiaofuge.deepseek.harness.api.dto.agent.AgentMessageRequest;
import cn.xiaofuge.deepseek.harness.api.dto.agent.AgentMessageResponse;
import cn.xiaofuge.deepseek.harness.cases.agent.node.AgentResolveNode;
import cn.xiaofuge.deepseek.harness.domain.shared.strategy.StrategyHandler;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Agent 消息策略树工厂。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class AgentMessageFactory {

    @Autowired
    @Qualifier("agentMessageResolveNode")
    private AgentResolveNode rootNode;

    public StrategyHandler<AgentMessageRequest, AgentMessageDynamicContext, AgentMessageResponse> strategyHandler() {
        return rootNode;
    }

    public AgentMessageDynamicContext newContext() {
        return new AgentMessageDynamicContext();
    }
}
