package cn.xiaofuge.deepseek.harness.trigger.service.stream;

import cn.xiaofuge.deepseek.harness.api.IAgentApi;
import cn.xiaofuge.deepseek.harness.api.IWorkflowCommandApi;
import cn.xiaofuge.deepseek.harness.api.dto.AgentMessageRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.AgentMessageResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.StartWorkflowRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.WorkflowStartResponseDTO;
import cn.xiaofuge.deepseek.harness.api.gateway.IGatewayStreamApi;
import cn.xiaofuge.deepseek.harness.api.gateway.dto.GatewayStreamEventDTO;
import cn.xiaofuge.deepseek.harness.api.gateway.dto.GatewayStreamRequestDTO;
import cn.xiaofuge.deepseek.harness.api.response.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.io.IOException;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class GatewayStreamApi implements IGatewayStreamApi {

    private static final Logger log = LoggerFactory.getLogger(GatewayStreamApi.class);

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
        String requestId = UUID.randomUUID().toString();
        StreamContext context = new StreamContext(
                requestId,
                Instant.now(),
                emitter,
                new java.util.concurrent.atomic.AtomicBoolean(false),
                new java.util.concurrent.atomic.AtomicBoolean(false)
        );
        log.info("[GatewayStream] 接收数字人网关流式请求，requestId={}，{}", requestId, summarizeRequest(request));
        emitter.onCompletion(() -> {
            context.closed().set(true);
            log.info("[GatewayStream] SSE 客户端连接完成，requestId={}，elapsedMs={}",
                    requestId, elapsedMillis(context));
        });
        emitter.onTimeout(() -> {
            log.warn("[GatewayStream] SSE 连接超时，requestId={}，elapsedMs={}",
                    requestId, elapsedMillis(context));
            complete(context);
        });
        emitter.onError(error -> {
            context.closed().set(true);
            log.warn("[GatewayStream] SSE 客户端连接异常，requestId={}，elapsedMs={}，error={}",
                    requestId, elapsedMillis(context), error.getMessage());
        });
        streamExecutor.execute(() -> streamRequest(request, context));
        return emitter;
    }

    private void streamRequest(GatewayStreamRequestDTO request, StreamContext context) {
        try {
            if (request == null) {
                throw new IllegalArgumentException("gateway stream request is required");
            }
            log.info("[GatewayStream] 开始分发数字人网关请求，requestId={}，source={}",
                    context.requestId(), request.source());
            switch (request.source().toLowerCase(Locale.ROOT)) {
                case "agent" -> streamAgent(request.source(), request, context);
                case "workflow" -> streamWorkflow(request.source(), request, context);
                default -> throw new IllegalArgumentException("unsupported gateway source: " + request.source());
            }
        } catch (Exception exception) {
            String source = request == null ? "unknown" : request.source();
            log.error("[GatewayStream] 数字人网关请求处理失败，requestId={}，source={}，elapsedMs={}",
                    context.requestId(), source, elapsedMillis(context), exception);
            sendEvent(source, context, "error", Map.of(
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
        log.info("[GatewayStream] Agent 流式调用开始，requestId={}，agentId={}，channelCode={}，messageLength={}，imageCount={}",
                context.requestId(),
                request.agent().agentId(),
                blankToDefault(request.agent().channelCode(), "<active>"),
                length(request.agent().message()),
                request.agent().images() == null ? 0 : request.agent().images().size());
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
        log.info("[GatewayStream] Agent 流式调用完成，requestId={}，agentId={}，elapsedMs={}",
                context.requestId(), request.agent().agentId(), elapsedMillis(context));
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
        log.info("[GatewayStream] Workflow 流式调用开始，requestId={}，script={}，name={}，parentCwd={}，argCount={}",
                context.requestId(),
                blankToDefault(request.workflow().script(), "<blank>"),
                blankToDefault(request.workflow().name(), "<blank>"),
                blankToDefault(request.workflow().parentCwd(), "<blank>"),
                request.workflow().args() == null ? 0 : request.workflow().args().size());
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
        log.info("[GatewayStream] Workflow 流式调用完成，requestId={}，script={}，elapsedMs={}",
                context.requestId(), blankToDefault(request.workflow().script(), "<blank>"), elapsedMillis(context));
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
            log.warn("[GatewayStream] SSE 事件发送失败，requestId={}，source={}，event={}，elapsedMs={}",
                    context.requestId(), source, event, elapsedMillis(context));
        }
    }

    private void complete(StreamContext context) {
        if (context.closed().compareAndSet(false, true)) {
            log.info("[GatewayStream] SSE 连接关闭，requestId={}，completed={}，elapsedMs={}",
                    context.requestId(), context.completed().get(), elapsedMillis(context));
            context.emitter().complete();
        }
    }

    private void completeWithError(StreamContext context, Exception exception) {
        if (context.closed().compareAndSet(false, true)) {
            log.warn("[GatewayStream] SSE 异常关闭，requestId={}，completed={}，elapsedMs={}，error={}",
                    context.requestId(), context.completed().get(), elapsedMillis(context), exception.getMessage());
            context.emitter().completeWithError(exception);
        }
    }

    private String summarizeRequest(GatewayStreamRequestDTO request) {
        if (request == null) {
            return "source=<null>，requestType=unknown";
        }
        if (request.agent() != null) {
            return summarizeAgentRequest(request.source(), request.agent());
        }
        if (request.workflow() != null) {
            return summarizeWorkflowRequest(request.source(), request.workflow());
        }
        return "source=" + request.source() + "，requestType=empty";
    }

    private String summarizeAgentRequest(String source, AgentMessageRequestDTO request) {
        return "source=" + source
                + "，requestType=agent"
                + "，agentId=" + request.agentId()
                + "，channelCode=" + blankToDefault(request.channelCode(), "<active>")
                + "，cwd=" + blankToDefault(request.cwd(), "<default>")
                + "，messageLength=" + length(request.message())
                + "，imageCount=" + (request.images() == null ? 0 : request.images().size())
                + "，approvalMode=" + blankToDefault(request.approvalMode(), "<default>")
                + "，reasoningEffort=" + blankToDefault(request.reasoningEffort(), "<default>")
                + "，sandboxRootCount=" + (request.sandboxRoots() == null ? 0 : request.sandboxRoots().size());
    }

    private String summarizeWorkflowRequest(String source, StartWorkflowRequestDTO request) {
        return "source=" + source
                + "，requestType=workflow"
                + "，script=" + blankToDefault(request.script(), "<blank>")
                + "，name=" + blankToDefault(request.name(), "<blank>")
                + "，parentCwd=" + blankToDefault(request.parentCwd(), "<blank>")
                + "，argCount=" + (request.args() == null ? 0 : request.args().size());
    }

    private long elapsedMillis(StreamContext context) {
        return Duration.between(context.startedAt(), Instant.now()).toMillis();
    }

    private int length(String value) {
        return value == null ? 0 : value.length();
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private record StreamContext(
            String requestId,
            Instant startedAt,
            SseEmitter emitter,
            java.util.concurrent.atomic.AtomicBoolean completed,
            java.util.concurrent.atomic.AtomicBoolean closed
    ) {}
}
