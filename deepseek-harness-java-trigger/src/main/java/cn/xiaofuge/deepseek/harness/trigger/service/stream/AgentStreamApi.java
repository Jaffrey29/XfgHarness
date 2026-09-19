package cn.xiaofuge.deepseek.harness.trigger.service.stream;

import cn.xiaofuge.deepseek.harness.api.IAgentApi;
import cn.xiaofuge.deepseek.harness.api.dto.AgentMessageRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.AgentMessageResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.AgentToolCallEventResponseDTO;
import cn.xiaofuge.deepseek.harness.api.response.Response;
import cn.xiaofuge.deepseek.harness.api.stream.IAgentStreamApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class AgentStreamApi implements IAgentStreamApi {

    private static final Logger log = LoggerFactory.getLogger(AgentStreamApi.class);
    // 0 = 不限超时：agent 单轮可能远超 5 分钟（长推理/长工具），固定 300s 会中途掐断流；
    // 前端有 120s 空闲看门狗 + REST 对账兜底，服务端不需要再设硬超时。
    private static final long SSE_TIMEOUT_MS = 0L;

    private final IAgentApi agentApi;
    private final ObjectMapper objectMapper;
    private final ExecutorService sseExecutor;

    public AgentStreamApi(IAgentApi agentApi, ObjectMapper objectMapper) {
        this.agentApi = agentApi;
        this.objectMapper = objectMapper;
        // 弹性线程池：每条流占用一个工作线程直到该轮 agent turn 结束，
        // 固定核数池在多对话并发 + 前端停止后服务端继续跑完的场景下会耗尽线程，
        // 导致新流在首字节前排队。缓存的 daemon 线程池让并发上限随负载伸缩。
        this.sseExecutor = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "sse-stream-worker");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public SseEmitter sendMessageStreaming(AgentMessageRequestDTO request) {
        log.info("[SSE] 收到流式请求 agentId={} channel={}",
                request.agentId(), request.channelCode());
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        java.util.concurrent.atomic.AtomicBoolean emitterClosed =
                new java.util.concurrent.atomic.AtomicBoolean(false);
        emitter.onCompletion(() -> emitterClosed.set(true));
        emitter.onTimeout(emitter::complete);
        emitter.onError(error -> emitterClosed.set(true));

        sseExecutor.execute(() -> emitAgentStream(request, emitter, emitterClosed));
        return emitter;
    }

    private void emitAgentStream(
            AgentMessageRequestDTO request,
            SseEmitter emitter,
            java.util.concurrent.atomic.AtomicBoolean emitterClosed
    ) {
        java.util.concurrent.atomic.AtomicInteger chunkIndex =
                new java.util.concurrent.atomic.AtomicInteger(0);
        try {
            sendMeta(request, emitter);
            java.util.function.Consumer<String> deltaSink = delta -> sendDelta(
                    emitter, emitterClosed, chunkIndex, delta
            );
            java.util.function.Consumer<String> reasoningSink = delta -> sendReasoning(emitter, emitterClosed, delta);
            java.util.function.Consumer<AgentToolCallEventResponseDTO> toolCallSink = event ->
                    sendToolCall(emitter, emitterClosed, event);
            Runnable finishSink = () -> sendFinish(emitter, emitterClosed);

            Response<AgentMessageResponseDTO> wrappedResponse =
                    agentApi.sendMessageStreaming(request, deltaSink, reasoningSink, toolCallSink, finishSink);
            if (wrappedResponse.data() == null) {
                throw new IllegalStateException(wrappedResponse.info());
            }
            sendDone(emitter, emitterClosed, wrappedResponse.data(), chunkIndex.get());
        } catch (Exception exception) {
            sendError(emitter, emitterClosed, exception);
        }
    }

    private void sendMeta(AgentMessageRequestDTO request, SseEmitter emitter) throws IOException {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("agentId", request.agentId());
        emitter.send(SseEmitter.event().name("meta").data(objectMapper.writeValueAsString(meta)));
    }

    private void sendDelta(
            SseEmitter emitter,
            java.util.concurrent.atomic.AtomicBoolean emitterClosed,
            java.util.concurrent.atomic.AtomicInteger chunkIndex,
            String delta
    ) {
        if (emitterClosed.get()) {
            return;
        }
        try {
            Map<String, Object> chunkData = new LinkedHashMap<>();
            chunkData.put("content", delta);
            chunkData.put("index", chunkIndex.getAndIncrement());
            emitter.send(SseEmitter.event().name("chunk").data(objectMapper.writeValueAsString(chunkData)));
        } catch (IOException exception) {
            emitterClosed.set(true);
        }
    }

    private void sendReasoning(
            SseEmitter emitter,
            java.util.concurrent.atomic.AtomicBoolean emitterClosed,
            String delta
    ) {
        if (emitterClosed.get()) return;
        try {
            Map<String, Object> reasoningData = new LinkedHashMap<>();
            reasoningData.put("content", delta);
            emitter.send(SseEmitter.event().name("reasoning").data(objectMapper.writeValueAsString(reasoningData)));
        } catch (IOException exception) {
            emitterClosed.set(true);
        }
    }

    private void sendToolCall(
            SseEmitter emitter,
            java.util.concurrent.atomic.AtomicBoolean emitterClosed,
            AgentToolCallEventResponseDTO event
    ) {
        if (emitterClosed.get()) {
            return;
        }
        try {
            boolean isResult = "result".equals(event.phase());
            log.info("[SSE] 推送 {} 事件：toolName={} callId={}",
                    isResult ? "tool_result" : "step_break", event.toolName(), event.callId());
            Map<String, Object> toolCallData = new LinkedHashMap<>();
            toolCallData.put("toolName", event.toolName());
            toolCallData.put("callId", event.callId());
            toolCallData.put("args", event.arguments());
            toolCallData.put("status", event.status());
            if (event.result() != null) toolCallData.put("result", event.result());
            if (event.durationMs() != null) toolCallData.put("durationMs", event.durationMs());
            emitter.send(SseEmitter.event()
                    .name(isResult ? "tool_result" : "step_break")
                    .data(objectMapper.writeValueAsString(toolCallData)));
        } catch (IOException exception) {
            emitterClosed.set(true);
            log.warn("[SSE] step_break 推送失败，emitter 已关闭：{}", exception.getMessage());
        }
    }

    private void sendFinish(
            SseEmitter emitter,
            java.util.concurrent.atomic.AtomicBoolean emitterClosed
    ) {
        if (emitterClosed.get()) return;
        try {
            emitter.send(SseEmitter.event().name("finish").data("{}"));
        } catch (IOException ignored) {
            emitterClosed.set(true);
        }
    }

    private void sendDone(
            SseEmitter emitter,
            java.util.concurrent.atomic.AtomicBoolean emitterClosed,
            AgentMessageResponseDTO response,
            int totalChunks
    ) throws IOException {
        if (emitterClosed.get()) {
            return;
        }
        Map<String, Object> done = new LinkedHashMap<>();
        done.put("agentId", response.agentId());
        done.put("sessionId", response.sessionId());
        done.put("status", response.status());
        done.put("messages", response.messages());
        done.put("totalChunks", totalChunks);
        log.info("[SSE] 流式完成，推送 done 事件：agentId={} sessionId={} status={} chunks={}",
                response.agentId(), response.sessionId(), response.status(), totalChunks);
        emitter.send(SseEmitter.event().name("done").data(objectMapper.writeValueAsString(done)));
        emitter.complete();
    }

    private void sendError(
            SseEmitter emitter,
            java.util.concurrent.atomic.AtomicBoolean emitterClosed,
            Exception exception
    ) {
        if (emitterClosed.get()) {
            emitter.complete();
            return;
        }
        log.error("[SSE] 流式处理异常：{}", exception.getMessage(), exception);
        try {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", exception.getClass().getSimpleName());
            error.put("message", exception.getMessage());
            emitter.send(SseEmitter.event().name("error").data(objectMapper.writeValueAsString(error)));
        } catch (IOException ignored) {
        }
        emitter.completeWithError(exception);
    }
}
