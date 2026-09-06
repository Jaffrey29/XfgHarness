package cn.xiaofuge.deepseek.harness.trigger.http;

import cn.xiaofuge.deepseek.harness.api.dto.agent.AgentMessageRequest;
import cn.xiaofuge.deepseek.harness.api.dto.agent.AgentMessageResponse;
import cn.xiaofuge.deepseek.harness.api.service.IAgentApi;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.ToolCallBlock;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 智能体交互控制器。
 * <p>
 * 执行流程：
 * 1. 接收前端的消息发送或状态查询请求；
 * 2. 调用智能体适配器完成会话编排或状态读取；
 * 3. 返回普通 JSON 或 SSE 流式响应。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    private static final long SSE_TIMEOUT_MS = 300_000L; // #3 加长到 5 分钟
    private static final int CHUNK_SIZE = 40;

    private final IAgentApi agentApi;
    private final ObjectMapper objectMapper;
    private final ExecutorService sseExecutor;

    public AgentController(IAgentApi agentApi, ObjectMapper objectMapper) {
        this.agentApi = agentApi;
        this.objectMapper = objectMapper;
        this.sseExecutor = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                r -> {
                    Thread t = new Thread(r, "sse-stream-worker");
                    t.setDaemon(true);
                    return t;
                }
        );
    }

    @PostMapping("/message")
    public AgentMessageResponse sendMessage(@RequestBody AgentMessageRequest request) {
        log.info("[HTTP] /api/agent/message 收到请求 agentId={}", request.agentId());
        return agentApi.sendMessage(request);
    }

    @PostMapping(value = "/stream")
    public org.springframework.http.ResponseEntity<SseEmitter> streamMessage(@RequestBody AgentMessageRequest request) {
        log.info("[HTTP] /api/agent/stream 收到流式请求 agentId={} provider={} model={}",
                request.agentId(), request.provider(), request.model());
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        emitter.onCompletion(() -> { });
        emitter.onTimeout(() -> emitter.complete());
        emitter.onError(emitter::completeWithError);

        sseExecutor.execute(() -> {
            java.util.concurrent.atomic.AtomicInteger chunkIndex = new java.util.concurrent.atomic.AtomicInteger(0);
            java.util.concurrent.atomic.AtomicBoolean emitterClosed = new java.util.concurrent.atomic.AtomicBoolean(false);

            try {
                Map<String, Object> meta = new LinkedHashMap<>();
                meta.put("agentId", request.agentId());
                emitter.send(SseEmitter.event()
                        .name("meta")
                        .data(objectMapper.writeValueAsString(meta)));

                // 流式增量发送：每次收到一段大模型输出，就立即推送给前端，
                // 不等待整轮会话结束。
                java.util.function.Consumer<String> deltaSink = delta -> {
                    if (emitterClosed.get()) return;
                    try {
                        Map<String, Object> chunkData = new LinkedHashMap<>();
                        chunkData.put("content", delta);
                        chunkData.put("index", chunkIndex.getAndIncrement());
                        emitter.send(SseEmitter.event()
                                .name("chunk")
                                .data(objectMapper.writeValueAsString(chunkData)));
                    } catch (IOException e) {
                        emitterClosed.set(true);
                    }
                };

                // 工具调用事件 sink：Agent 在 tool 执行前回调，
                // 我们推送独立的 step_break SSE 事件，让前端正确分段渲染。
                java.util.function.Consumer<ToolCallBlock> toolCallSink = tc -> {
                    if (emitterClosed.get()) return;
                    try {
                        log.info("[SSE] 推送 step_break 事件：toolName={} callId={}", tc.name(), tc.id());
                        Map<String, Object> tcData = new LinkedHashMap<>();
                        tcData.put("toolName", tc.name());
                        tcData.put("callId", tc.id());
                        tcData.put("args", tc.arguments());
                        tcData.put("status", "running");
                        emitter.send(SseEmitter.event()
                                .name("step_break")
                                .data(objectMapper.writeValueAsString(tcData)));
                    } catch (IOException e) {
                        emitterClosed.set(true);
                        log.warn("[SSE] step_break 推送失败，emitter 已关闭：{}", e.getMessage());
                    }
                };

                AgentMessageResponse response = agentApi.sendMessageStreaming(request, deltaSink, toolCallSink);

                Map<String, Object> done = new LinkedHashMap<>();
                done.put("agentId", response.agentId());
                done.put("sessionId", response.sessionId());
                done.put("status", response.status());
                done.put("messages", response.messages());
                done.put("totalChunks", chunkIndex.get());
                if (!emitterClosed.get()) {
                    log.info("[SSE] 流式完成，推送 done 事件：agentId={} sessionId={} status={} chunks={} messages={}",
                            response.agentId(), response.sessionId(), response.status(), chunkIndex.get(),
                            response.messages() == null ? 0 : response.messages().size());
                    emitter.send(SseEmitter.event()
                            .name("done")
                            .data(objectMapper.writeValueAsString(done)));
                    emitter.complete();
                }
            } catch (Exception e) {
                if (emitterClosed.get()) {
                    emitter.complete();
                    return;
                }
                log.error("[SSE] 流式处理异常：{}", e.getMessage(), e);
                try {
                    Map<String, Object> error = new LinkedHashMap<>();
                    error.put("error", e.getClass().getSimpleName());
                    error.put("message", e.getMessage());
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(objectMapper.writeValueAsString(error)));
                } catch (IOException ignored) {
                }
                emitter.completeWithError(e);
            }
        });

        // 强制 SSE 响应声明 UTF-8 charset。
        // 缺省情况下 SseEmitter 只发 "Content-Type: text/event-stream"（无 charset），
        // 部分浏览器/中间层会按单字节编码解码多字节中文，导致流式文本切碎乱码。
        return org.springframework.http.ResponseEntity.ok()
                .contentType(new MediaType("text", "event-stream", java.nio.charset.StandardCharsets.UTF_8))
                .body(emitter);
    }

    @GetMapping("/{agentId}/status")
    public String getAgentStatus(@PathVariable("agentId") String agentId) {
        return agentApi.getAgentStatus(agentId);
    }

    @PostMapping("/{agentId}/cancel")
    public void cancelAgent(@PathVariable("agentId") String agentId) {
        log.info("[HTTP] 收到取消请求 agentId={}", agentId);
        agentApi.cancelAgent(agentId);
    }

    private String extractAssistantText(AgentMessageResponse response) {
        if (response.messages() == null || response.messages().isEmpty()) {
            return "";
        }
        for (int i = response.messages().size() - 1; i >= 0; i--) {
            Map<String, Object> msg = response.messages().get(i);
            Object role = msg.get("role");
            if ("assistant".equals(role)) {
                Object content = msg.get("content");
                if (content instanceof String s) {
                    return s;
                }
                if (content instanceof List<?> list) {
                    StringBuilder sb = new StringBuilder();
                    for (Object item : list) {
                        if (item instanceof Map<?, ?> m && "text".equals(m.get("type"))) {
                            Object text = m.get("text");
                            if (text != null) {
                                sb.append(text);
                            }
                        }
                    }
                    return sb.toString();
                }
            }
        }
        return response.messages().toString();
    }
}
