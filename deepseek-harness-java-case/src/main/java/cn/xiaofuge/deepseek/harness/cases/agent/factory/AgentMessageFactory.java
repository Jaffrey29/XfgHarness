package cn.xiaofuge.deepseek.harness.cases.agent.factory;

import cn.xiaofuge.deepseek.harness.cases.agent.SendMessageCommand;
import cn.xiaofuge.deepseek.harness.cases.agent.SendMessageResult;
import cn.xiaofuge.deepseek.harness.cases.agent.node.AgentResolveNode;
import cn.xiaofuge.deepseek.harness.domain.shared.StrategyHandler;
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

    public StrategyHandler<SendMessageCommand, AgentMessageDynamicContext, SendMessageResult> strategyHandler() {
        return rootNode;
    }

    public AgentMessageDynamicContext newContext() {
        return new AgentMessageDynamicContext();
    }
}
