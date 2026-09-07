package cn.xiaofuge.deepseek.harness.cases.agent.factory;

import cn.xiaofuge.deepseek.harness.domain.agent.service.ReactLoopAgent;
import cn.xiaofuge.deepseek.harness.domain.model.entity.Message;

/**
 * Agent 消息策略树中流转的可变上下文。
 *
 * <p>调用流程：HTTP/API 门面 → AgentUseCase → AgentMessageFactory → 策略树 → ReactLoopAgent；会话事件写入 SessionEventLog。</p>
 * <p>示例：调用方传入 agentId、用户消息和可选模型配置，返回助手文本与工具调用结果。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public class AgentMessageDynamicContext {

    /**
     * MessageIntent 定义当前模块中的enum契约与职责。
     */
    public enum MessageIntent {
        CHAT,
        CODE_QUESTION,
        TASK_EXECUTION,
        CLARIFICATION
    }

    private ReactLoopAgent agent;
    private Message userMessage;
    private MessageIntent intent;

    /**
     * 获取Agent。
     */
    public ReactLoopAgent getAgent() { return agent; }
    /**
     * 设置Agent。
     */
    public void setAgent(ReactLoopAgent agent) { this.agent = agent; }

    /**
     * 获取User Message。
     */
    public Message getUserMessage() { return userMessage; }
    /**
     * 设置User Message。
     */
    public void setUserMessage(Message userMessage) { this.userMessage = userMessage; }

    /**
     * 获取Intent。
     */
    public MessageIntent getIntent() { return intent; }
    /**
     * 设置Intent。
     */
    public void setIntent(MessageIntent intent) { this.intent = intent; }
}
