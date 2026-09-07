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
 * <p>核心流程：UseCase 先取根策略节点，再按 Resolve -> Intent -> Dispatch -> Collect 处理消息。
 * <p>数据示例：用户发送“帮我查看 /tmp/a.txt”，Resolve 复用或创建 Agent，Intent 识别为任务/文件意图，
 * Dispatch 交给 ReactLoopAgent 调用文件工具，Collect 汇总最终回复。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class AgentMessageFactory {

    @Autowired
    @Qualifier("agentMessageResolveNode")
    private AgentResolveNode rootNode;

    /**
     * 返回 Agent 消息策略树的根节点。
     * <p>根节点固定为 {@link AgentResolveNode}，后续节点由每个节点自己的 getNext 规则连接。
     *
     * @return 可直接 apply 的消息处理策略
     */
    public StrategyHandler<SendMessageCommand, AgentMessageDynamicContext, SendMessageResult> strategyHandler() {
        return rootNode;
    }

    /**
     * 创建一次消息处理过程中的动态上下文。
     * <p>上下文会承载解析出的 Agent 实例和消息意图，贯穿整棵策略树。
     *
     * @return 空白动态上下文
     */
    public AgentMessageDynamicContext newContext() {
        return new AgentMessageDynamicContext();
    }
}
