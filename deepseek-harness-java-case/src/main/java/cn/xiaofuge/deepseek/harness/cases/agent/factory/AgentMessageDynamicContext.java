package cn.xiaofuge.deepseek.harness.cases.agent.factory;

import cn.xiaofuge.deepseek.harness.domain.agent.service.ReactLoopAgent;
import cn.xiaofuge.deepseek.harness.domain.model.entity.Message;

/**
 * Agent 消息策略树中流转的可变上下文。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class AgentMessageDynamicContext {

    public enum MessageIntent {
        CHAT,
        CODE_QUESTION,
        TASK_EXECUTION,
        CLARIFICATION
    }

    private ReactLoopAgent agent;
    private Message userMessage;
    private MessageIntent intent;

    public ReactLoopAgent getAgent() { return agent; }
    public void setAgent(ReactLoopAgent agent) { this.agent = agent; }

    public Message getUserMessage() { return userMessage; }
    public void setUserMessage(Message userMessage) { this.userMessage = userMessage; }

    public MessageIntent getIntent() { return intent; }
    public void setIntent(MessageIntent intent) { this.intent = intent; }
}
