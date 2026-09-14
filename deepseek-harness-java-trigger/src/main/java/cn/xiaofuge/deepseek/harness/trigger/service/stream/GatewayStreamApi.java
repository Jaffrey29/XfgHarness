package cn.xiaofuge.deepseek.harness.trigger.service.stream;

import cn.xiaofuge.deepseek.harness.api.IAgentApi;
import cn.xiaofuge.deepseek.harness.api.IWorkflowCommandApi;
import cn.xiaofuge.deepseek.harness.api.dto.AgentMessageResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.WorkflowStartResponseDTO;
import cn.xiaofuge.deepseek.harness.api.gateway.IGatewayStreamApi;
import cn.xiaofuge.deepseek.harness.api.gateway.dto.GatewayStreamEventDTO;
import cn.xiaofuge.deepseek.harness.api.gateway.dto.GatewayStreamRequestDTO;
import cn.xiaofuge.deepseek.harness.api.response.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class GatewayStreamApi implements IGatewayStreamApi {

    private final IAgentApi agentApi;
    private final IWorkflowCommandApi workflowCommandApi;
    private final ObjectMapper objectMapper;
    private final ExecutorService streamExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            runnable -> {
                Thread thread = new Thread(runnable, "gateway-stream-worker");
                thread.setDaemon(true);
                return thread;
            }
    );

    public GatewayStreamApi(
            IAgentApi agentApi,
            IWorkflowCommandApi workflowCommandApi,
            ObjectMapper objectMapper
    ) {
        this.agentApi = agentApi;
        this.workflowCommandApi = workflowCommandApi;
        this.objectMapper = objectMapper;
    }

    @Override
    public SseEmitter stream(GatewayStreamRequestDTO request) {
        SseEmitter emitter = new SseEmitter(300_000L);
        StreamContext context = new StreamContext(
                emitter,
                new java.util.concurrent.atomic.AtomicBoolean(false),
                new java.util.concurrent.atomic.AtomicBoolean(false)
        );
        emitter.onCompletion(() -> context.closed().set(true));
        emitter.onTimeout(() -> complete(context));
        emitter.onError(error -> context.closed().set(true));
        streamExecutor.execute(() -> streamRequest(request, context));
        return emitter;
    }

    private void streamRequest(GatewayStreamRequestDTO request, StreamContext context) {
        try {
            switch (request.source().toLowerCase()) {
                case "agent" -> streamAgent(request.source(), request, context);
                case "workflow" -> streamWorkflow(request.source(), request, context);
                default -> throw new IllegalArgumentException("unsupported gateway source: " + request.source());
            }
        } catch (Exception exception) {
            sendEvent(request.source(), context, "error", Map.of(
                    "error", exception.getClass().getSimpleName(),
                    "message", exception.getMessage() == null ? "" : exception.getMessage()
            ));
            completeWithError(context, exception);
        }
    }

    private void streamAgent(
            String source,
            GatewayStreamRequestDTO request,
            StreamContext context
    ) {
        if (request.agent() == null) {
            throw new IllegalArgumentException("agent request is required");
        }
        Response<AgentMessageResponseDTO> response = agentApi.sendMessageStreaming(
                request.agent(),
                delta -> sendEvent(source, context, "message.delta", delta),
                delta -> sendEvent(source, context, "message.reasoning", delta),
                toolCall -> sendEvent(source, context,
                        toolCall.phase().equals("result") ? "tool.result" : "tool.call", toolCall),
                () -> sendEvent(source, context, "finish", Map.of())
        );
        if (!Response.SUCCESS_CODE.equals(response.code())) {
            throw new IllegalStateException(response.info());
        }
        sendEvent(source, context, "done", response.data());
        context.completed().set(true);
        complete(context);
    }

    private void streamWorkflow(
            String source,
            GatewayStreamRequestDTO request,
            StreamContext context
    ) {
        if (request.workflow() == null) {
            throw new IllegalArgumentException("workflow request is required");
        }
        Response<WorkflowStartResponseDTO> response = workflowCommandApi.startStreaming(
                request.workflow(),
                event -> {
                    sendEvent(source, context, event.event(), event);
                    if ("done".equals(event.event()) || "error".equals(event.event())) {
                        context.completed().set(true);
                        complete(context);
                    }
                }
        );
        if (!Response.SUCCESS_CODE.equals(response.code())) {
            throw new IllegalStateException(response.info());
        }
        if (!context.completed().get()) {
            sendEvent(source, context, "done", response.data());
            context.completed().set(true);
        }
        complete(context);
    }

    private void sendEvent(String source, StreamContext context, String event, Object payload) {
        if (context.closed().get()) {
            return;
        }
        try {
            GatewayStreamEventDTO envelope = new GatewayStreamEventDTO(source, event, Instant.now(), payload);
            context.emitter().send(SseEmitter.event().name(event).data(objectMapper.writeValueAsString(envelope)));
        } catch (IOException ignored) {
            context.closed().set(true);
        }
    }

    private void complete(StreamContext context) {
        if (context.closed().compareAndSet(false, true)) {
            context.emitter().complete();
        }
    }

    private void completeWithError(StreamContext context, Exception exception) {
        if (context.closed().compareAndSet(false, true)) {
            context.emitter().completeWithError(exception);
        }
    }

    private record StreamContext(
            SseEmitter emitter,
            java.util.concurrent.atomic.AtomicBoolean completed,
            java.util.concurrent.atomic.AtomicBoolean closed
    ) {}
}
