package cn.xiaofuge.deepseek.harness.cases.agent.node;

import cn.xiaofuge.deepseek.harness.api.dto.AgentMessageRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.AgentMessageResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.agent.factory.AgentMessageDynamicContext;
import cn.xiaofuge.deepseek.harness.domain.model.entity.Message;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.ContentBlock;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.TextBlock;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.ReasoningBlock;
import cn.xiaofuge.deepseek.harness.domain.model.entity.MessageSource;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.ToolResultBlock;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionEvent;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionLog;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 收集节点：等待 Agent 空闲后，从会话事件日志收集对话消息。
 * 这是 Agent 消息策略树的终止节点。
 *
 * <p>调用流程：HTTP/API 门面 → AgentUseCase → AgentMessageFactory → 策略树 → ReactLoopAgent；会话事件写入 SessionEventLog。</p>
 * <p>示例：调用方传入 agentId、用户消息和可选模型配置，返回助手文本与工具调用结果。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service("agentMessageCollectNode")
public class AgentCollectNode extends AbstractAgentMessageNode {

    private static final Logger log = LoggerFactory.getLogger(AgentCollectNode.class);

    /**
     * 等待 Agent 执行完成，并把会话事件收集为响应消息。
     * <p>执行流程：whenIdle().join() 等待收尾 -> 遍历 SessionLog 事件 -> 用户/助手直接展平文本，
     * ToolCall 先建 running 消息，ToolResult 通过 callId 回填最终 status/result。
     * <p>数据示例：callId=call_1 的 fs_read 调用会先出现 status=running，
     * 成功结果到达后同一卡片变为 status=success 并携带文件内容。
     */
    @Override
    protected AgentMessageResponseDTO doApply(AgentMessageRequestDTO request,
                                           AgentMessageDynamicContext ctx) throws Exception {
        var agent = ctx.getAgent();
        log.info("[Collect] Agent={} 等待 Agent 空闲，开始收集会话结果...", request.agentId());
        try {
            agent.whenIdle().join();
        } catch (Exception e) {
            // 超时或异常时，直接返回已收集到的消息
            log.warn("[Collect] Agent={} 等待空闲超时/异常，返回已收集消息：{}", request.agentId(), e.getMessage());
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

        log.info("[Collect] Agent={} 收集完成，共 {} 条消息，status={}",
                request.agentId(), messages.size(), agent.status().name().toLowerCase());
        return new AgentMessageResponseDTO(
                request.agentId(),
                session.sessionId(),
                agent.status().name().toLowerCase(),
                messages
        );
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
        return defaultStrategyHandler;
    }

    /**
     * 将助手消息中的推理块和正文块转为可读文本。
     * <p>两者同时存在时使用 HTML details 折叠推理内容，前端可直接展示 Markdown。
     */
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
