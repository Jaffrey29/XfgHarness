package cn.xiaofuge.deepseek.harness.cases.agent.node;

import cn.xiaofuge.deepseek.harness.api.dto.agent.AgentMessageRequest;
import cn.xiaofuge.deepseek.harness.api.dto.agent.AgentMessageResponse;
import cn.xiaofuge.deepseek.harness.cases.agent.factory.AgentMessageDynamicContext;
import cn.xiaofuge.deepseek.harness.domain.model.entity.Message;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.ContentBlock;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.TextBlock;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.ReasoningBlock;
import cn.xiaofuge.deepseek.harness.domain.model.entity.MessageSource;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.ToolResultBlock;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionEvent;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionLog;
import cn.xiaofuge.deepseek.harness.domain.shared.strategy.StrategyHandler;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 收集节点：等待 Agent 空闲后，从会话事件日志收集对话消息。
 * 这是 Agent 消息策略树的终止节点。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service("agentMessageCollectNode")
public class AgentCollectNode extends AbstractAgentMessageNode {

    @Override
    protected AgentMessageResponse doApply(AgentMessageRequest request,
                                           AgentMessageDynamicContext ctx) throws Exception {
        var agent = ctx.getAgent();
        try {
            agent.whenIdle().join();
        } catch (Exception e) {
            // 超时或异常时，直接返回已收集到的消息
        }

        SessionLog session = agent.session();
        // 按事件时间顺序构建消息；ToolCall 与 ToolResult 通过 callId 关联到同一消息。
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Map<String, Object>> toolCalls = new LinkedHashMap<>();
        for (SessionEvent event : session.events()) {
            if (event instanceof SessionEvent.UserMessage um) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("role", "user");
                m.put("content", extractText(um.data().message()));
                messages.add(m);
            } else if (event instanceof SessionEvent.AssistantMessage am) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("role", "assistant");
                m.put("content", extractText(am.data().message()));
                messages.add(m);
            } else if (event instanceof SessionEvent.ToolCall tc) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("role", "tool");
                m.put("toolName", tc.data().name());
                m.put("callId", tc.data().callId());
                m.put("args", tc.data().arguments());
                m.put("status", "running");
                toolCalls.put(tc.data().callId(), m);
                messages.add(m);
            } else if (event instanceof SessionEvent.ToolResult tr) {
                // 从 MessageSource.ToolMessageSource 取出真正的 callId（不是 Message.id）
                String callId = null;
                if (tr.data().message() != null
                        && tr.data().message().source() instanceof MessageSource.ToolMessageSource tms) {
                    callId = tms.callId();
                }
                Map<String, Object> m = callId != null ? toolCalls.get(callId) : null;
                if (m == null) {
                    m = new LinkedHashMap<>();
                    m.put("role", "tool");
                    m.put("toolName", "unknown");
                    messages.add(m);
                }
                m.put("status", tr.data().error() != null ? "error" : "success");
                String output = tr.data().message() != null && tr.data().message().content() != null
                        ? extractToolResultText(tr.data().message().content())
                        : "";
                if (output.length() > 8192) output = output.substring(0, 8192) + "...[truncated]";
                m.put("result", output);
            }
        }

        return new AgentMessageResponse(
                request.agentId(),
                session.sessionId(),
                agent.status().name().toLowerCase(),
                messages
        );
    }

    @Override
    public StrategyHandler<AgentMessageRequest, AgentMessageDynamicContext, AgentMessageResponse>
    getNext(AgentMessageRequest request, AgentMessageDynamicContext ctx) {
        return defaultStrategyHandler;
    }

    private String extractText(Message message) {
        StringBuilder reasoning = new StringBuilder();
        StringBuilder text = new StringBuilder();
        for (ContentBlock block : message.content()) {
            if (block instanceof TextBlock tb) {
                text.append(tb.text());
            } else if (block instanceof ReasoningBlock rb) {
                if (!reasoning.isEmpty()) reasoning.append("\n");
                reasoning.append(rb.text());
            }
        }
        if (!reasoning.isEmpty() && !text.isEmpty()) {
            return "<think>" + reasoning + "</think>\n" + text;
        } else if (!reasoning.isEmpty()) {
            return "<think>" + reasoning + "</think>";
        }
        return text.toString();
    }

    /**
     * 把工具结果的内容块展平成可读文本。
     * 工具结果 Message 的 content 是 ToolResultBlock（里面又嵌套 TextBlock 列表），
     * 直接 toString 会得到 "ToolResultBlock[toolCallId=..., ...]" 这种 Java 调试格式，前端不可读。
     */
    private String extractToolResultText(List<ContentBlock> blocks) {
        StringBuilder sb = new StringBuilder();
        for (ContentBlock block : blocks) {
            if (block instanceof ToolResultBlock trb) {
                for (ContentBlock inner : trb.content()) {
                    if (inner instanceof TextBlock tb) {
                        if (!sb.isEmpty()) sb.append("\n");
                        sb.append(tb.text());
                    }
                }
            } else if (block instanceof TextBlock tb) {
                if (!sb.isEmpty()) sb.append("\n");
                sb.append(tb.text());
            }
        }
        return sb.toString();
    }
}
