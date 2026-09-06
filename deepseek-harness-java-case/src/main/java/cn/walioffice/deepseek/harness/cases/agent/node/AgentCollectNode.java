package cn.walioffice.deepseek.harness.cases.agent.node;

import cn.walioffice.deepseek.harness.api.dto.agent.AgentMessageRequest;
import cn.walioffice.deepseek.harness.api.dto.agent.AgentMessageResponse;
import cn.walioffice.deepseek.harness.cases.agent.factory.AgentMessageDynamicContext;
import cn.walioffice.deepseek.harness.domain.llm.model.entity.Message;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.ContentBlock;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.TextBlock;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.ReasoningBlock;
import cn.walioffice.deepseek.harness.domain.session.event.model.entity.SessionEvent;
import cn.walioffice.deepseek.harness.domain.session.event.model.entity.SessionLog;
import cn.walioffice.deepseek.harness.domain.shared.strategy.StrategyHandler;
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
                        && tr.data().message().source() instanceof cn.walioffice.deepseek.harness.domain.llm.model.entity.MessageSource.ToolMessageSource tms) {
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
                        ? tr.data().message().content().toString()
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
}
