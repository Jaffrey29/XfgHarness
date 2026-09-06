package cn.walioffice.deepseek.harness.cases.agent.node;

import cn.walioffice.deepseek.harness.api.dto.agent.AgentMessageRequest;
import cn.walioffice.deepseek.harness.api.dto.agent.AgentMessageResponse;
import cn.walioffice.deepseek.harness.cases.agent.factory.AgentMessageDynamicContext;
import cn.walioffice.deepseek.harness.domain.shared.strategy.AbstractStrategyRouter;

/**
 * Agent 消息策略树节点的抽象基类。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public abstract class AbstractAgentMessageNode
        extends AbstractStrategyRouter<AgentMessageRequest, AgentMessageDynamicContext, AgentMessageResponse> {
}
