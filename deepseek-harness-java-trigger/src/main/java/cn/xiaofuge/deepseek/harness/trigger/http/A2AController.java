package cn.xiaofuge.deepseek.harness.trigger.http;

import cn.xiaofuge.deepseek.harness.api.IAgentApi;
import cn.xiaofuge.deepseek.harness.api.dto.AgentMessageRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.AgentToolCallEventResponseDTO;
import cn.xiaofuge.deepseek.harness.api.response.Response;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * A2A 协议 JSON-RPC 端点：本服务作为标准 A2A Server 对外提供 Agent 能力。
 *
 * <p>与 A2A 规范（0.3.x，JSON-RPC 绑定）保持一致：</p>
 * <ul>
 *   <li>{@code message/send}：阻塞式发消息，返回最终 {@code Task}（kind=task）</li>
 *   <li>{@code message/stream}：SSE 流式发消息，逐条推送 JSON-RPC 响应：
 *       初始 {@code Task} → {@code TaskArtifactUpdateEvent}（kind=artifact-update，append/lastChunk）
 *       → 终态 {@code TaskStatusUpdateEvent}（kind=status-update，final）</li>
 *   <li>{@code tasks/get}：查询任务（未找到返回 -32001 TaskNotFound）</li>
 *   <li>{@code tasks/cancel}：取消任务（终态任务返回 -32002 TaskNotCancelable）</li>
 * </ul>
 *
 * <p>结构约定（与规范示例一致）：TaskState 使用小写连字符
 * （submitted/working/completed/failed/canceled/input-required/auth-required/rejected）；
 * Message/Task/Artifact 均带 kind 判别字段；Part 为 {kind: text|file|data} 联合类型；
 * Agent Card（AgentCardController）的 url 字段指向本端点。</p>
 *
 * <p>内部映射：contextId 复用即同一 Agent 会话（多轮对话）；执行转调 {@link IAgentApi}，
 * delta/reasoning/tool 事件映射为 A2A 工件与状态更新事件。</p>
 */
@RestController
@RequestMapping("/a2a")
public class A2AController {

    private static final Logger log = LoggerFactory.getLogger(A2AController.class);
    private static final long SSE_TIMEOUT_MS = 600_000L;

    /** JSON-RPC / A2A 标准错误码 */
    private static final int PARSE_ERROR = -32700;
    private static final int INVALID_REQUEST = -32600;
    private static final int METHOD_NOT_FOUND = -32601;
    private static final int INVALID_PARAMS = -32602;
    private static final int TASK_NOT_FOUND = -32001;
    private static final int TASK_NOT_CANCELABLE = -32002;

    private final IAgentApi agentApi;
    private final ObjectMapper objectMapper;
    private final ExecutorService a2aExecutor;
    /** taskId -> 任务状态（内存态；进程重启即失，A2A 任务生命周期本就短于进程） */
    private final Map<String, A2aTask> tasks = new ConcurrentHashMap<>();

    public A2AController(IAgentApi agentApi, ObjectMapper objectMapper) {
        this.agentApi = agentApi;
        this.objectMapper = objectMapper;
        this.a2aExecutor = Executors.newFixedThreadPool(
                Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
                runnable -> {
                    Thread thread = new Thread(runnable, "a2a-worker");
                    thread.setDaemon(true);
                    return thread;
                });
    }

    // ── JSON-RPC 入口 ──────────────────────────────

    @PostMapping
    public Object handle(@RequestBody(required = false) JsonNode request) {
        if (request == null || request.isMissingNode()
                || !"2.0".equals(request.path("jsonrpc").asText(null))
                || request.path("method").asText(null) == null) {
            return errorResponse(idOf(request), INVALID_REQUEST, "Invalid Request");
        }
        JsonNode id = idOf(request);
        String method = request.path("method").asText();
        JsonNode params = request.path("params");
        log.info("[A2A] 收到请求 method={} id={}", method, id);
        return switch (method) {
            case "message/send" -> messageSend(id, params);
            case "message/stream" -> messageStream(id, params);
            case "tasks/get" -> tasksGet(id, params);
            case "tasks/cancel" -> tasksCancel(id, params);
            default -> errorResponse(id, METHOD_NOT_FOUND, "Method not found: " + method);
        };
    }

    /** 非法 JSON 请求体按 JSON-RPC 规范返回 -32700 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Map<String, Object> handleUnreadable(HttpMessageNotReadableException exception) {
        log.warn("[A2A] 请求体解析失败：{}", exception.getMessage());
        return errorResponse(null, PARSE_ERROR, "Parse error");
    }

    // ── message/send（阻塞式） ──────────────────────

    private Object messageSend(JsonNode id, JsonNode params) {
        JsonNode messageNode = params.path("message");
        String text = extractText(messageNode);
        if (messageNode.isMissingNode() || text.isBlank()) {
            return errorResponse(id, INVALID_PARAMS, "Invalid params: message with text parts is required");
        }
        A2aTask task = createTask(messageNode);
        try {
            execute(task, text, null, null, null);
            task.complete();
        } catch (Exception exception) {
            log.warn("[A2A] message/send 任务执行失败：{}", exception.getMessage());
            task.fail(exception.getMessage() == null ? "任务执行失败" : exception.getMessage());
        }
        return resultResponse(id, task.snapshot());
    }

    // ── message/stream（SSE 流式） ──────────────────

    private Object messageStream(JsonNode id, JsonNode params) {
        JsonNode messageNode = params.path("message");
        String text = extractText(messageNode);
        if (messageNode.isMissingNode() || text.isBlank()) {
            return errorResponse(id, INVALID_PARAMS, "Invalid params: message with text parts is required");
        }
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        a2aExecutor.execute(() -> streamTask(id, messageNode, text, emitter));
        return emitter;
    }

    private void streamTask(JsonNode id, JsonNode messageNode, String text, SseEmitter emitter) {
        AtomicBoolean closed = new AtomicBoolean(false);
        emitter.onCompletion(() -> closed.set(true));
        emitter.onTimeout(emitter::complete);
        emitter.onError(error -> closed.set(true));
        A2aTask task;
        try {
            task = createTask(messageNode);
        } catch (Exception exception) {
            sendResult(emitter, closed, id, failureStatusEvent("unknown", "unknown", exception));
            emitter.complete();
            return;
        }
        task.appendText(""); // 初始化时间戳
        try {
            // 规范：流首推初始 Task 对象（kind=task, status=working）
            sendResult(emitter, closed, id, task.snapshot());

            StringBuilder emitted = new StringBuilder();
            execute(task, text,
                    delta -> {
                        if (delta == null || delta.isEmpty()) return;
                        // 首个 chunk 以 append=false 起始新工件，其后 append=true 追加
                        boolean first = emitted.length() == 0;
                        emitted.append(delta);
                        sendResult(emitter, closed, id, artifactUpdateEvent(task, delta, !first, false));
                    },
                    toolEvent -> sendResult(emitter, closed, id, toolStatusEvent(task, toolEvent)),
                    () -> { /* finish 信号由收尾统一处理 */ });

            // 收尾：终态 status-update（final=true），流随之关闭
            task.complete();
            sendResult(emitter, closed, id, statusUpdateEvent(task, true, null));
        } catch (Exception exception) {
            log.warn("[A2A] 流式任务执行失败：{}", exception.getMessage());
            task.fail(exception.getMessage() == null ? "任务执行失败" : exception.getMessage());
            sendResult(emitter, closed, id, statusUpdateEvent(task, true, null));
        } finally {
            emitter.complete();
        }
    }

    /** 阻塞执行 Agent 并把结果写入任务（message/send 用） */
    private void execute(A2aTask task, String text,
                         java.util.function.Consumer<String> deltaSink,
                         java.util.function.Consumer<AgentToolCallEventResponseDTO> toolSink,
                         Runnable finishSink) {
        AgentMessageRequestDTO request = new AgentMessageRequestDTO(
                task.agentId(), null, null, null, text, null, "AUTO_APPROVE", null, null);
        java.util.function.Consumer<String> effectiveDeltaSink = deltaSink == null
                ? chunk -> task.appendText(chunk)
                : chunk -> {
                    task.appendText(chunk);
                    deltaSink.accept(chunk);
                };
        java.util.function.Consumer<AgentToolCallEventResponseDTO> effectiveToolSink;
        if (toolSink == null) {
            effectiveToolSink = toolEvent -> { };
        } else {
            effectiveToolSink = toolSink;
        }
        Runnable effectiveFinishSink = finishSink == null ? () -> { } : finishSink;
        Response<cn.xiaofuge.deepseek.harness.api.dto.AgentMessageResponseDTO> response =
                agentApi.sendMessageStreaming(request,
                        effectiveDeltaSink,
                        reasoning -> { /* A2A 无推理事件，忽略 */ },
                        effectiveToolSink,
                        effectiveFinishSink);
        if (response == null || response.data() == null) {
            throw new IllegalStateException(response == null ? "Agent 无响应" : response.info());
        }
    }

    // ── tasks/get、tasks/cancel ─────────────────────

    private Object tasksGet(JsonNode id, JsonNode params) {
        String taskId = params.path("id").asText(null);
        A2aTask task = taskId == null ? null : tasks.get(taskId);
        if (task == null) {
            return errorResponse(id, TASK_NOT_FOUND, "Task not found: " + taskId);
        }
        return resultResponse(id, task.snapshot());
    }

    private Object tasksCancel(JsonNode id, JsonNode params) {
        String taskId = params.path("id").asText(null);
        A2aTask task = taskId == null ? null : tasks.get(taskId);
        if (task == null) {
            return errorResponse(id, TASK_NOT_FOUND, "Task not found: " + taskId);
        }
        if (task.isTerminal()) {
            return errorResponse(id, TASK_NOT_CANCELABLE,
                    "Task cannot be canceled: already in terminal state " + task.state());
        }
        try {
            agentApi.cancelAgent(task.agentId());
        } catch (Exception exception) {
            log.warn("[A2A] 取消 Agent {} 失败：{}", task.agentId(), exception.getMessage());
        }
        task.cancel();
        return resultResponse(id, task.snapshot());
    }

    // ── 任务模型 ────────────────────────────────────

    /** 创建任务：contextId 复用即同一 Agent 会话（多轮），agentId 派生自 contextId */
    private A2aTask createTask(JsonNode messageNode) {
        String contextId = messageNode.path("contextId").asText("");
        if (contextId.isBlank()) {
            contextId = "ctx-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        }
        A2aTask task = new A2aTask("a2a-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12),
                contextId, "a2a-" + contextId, normalizeMessage(messageNode, "user"));
        tasks.put(task.id(), task);
        return task;
    }

    /** 按规范归一化 Message：role/parts/messageId/contextId/kind */
    private Map<String, Object> normalizeMessage(JsonNode messageNode, String fallbackRole) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", messageNode.path("role").asText(fallbackRole));
        message.put("parts", extractParts(messageNode));
        String messageId = messageNode.path("messageId").asText("");
        message.put("messageId", messageId.isBlank()
                ? "msg-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12)
                : messageId);
        String taskId = messageNode.path("taskId").asText("");
        if (!taskId.isBlank()) {
            message.put("taskId", taskId);
        }
        String contextId = messageNode.path("contextId").asText("");
        if (!contextId.isBlank()) {
            message.put("contextId", contextId);
        }
        message.put("kind", "message");
        return message;
    }

    /** 抽取 Parts：text 原样保留；data 序列化为文本；file 保留元信息 */
    private List<Map<String, Object>> extractParts(JsonNode messageNode) {
        List<Map<String, Object>> parts = new ArrayList<>();
        for (JsonNode part : messageNode.path("parts")) {
            String kind = part.path("kind").asText("text");
            Map<String, Object> normalized = new LinkedHashMap<>();
            switch (kind) {
                case "text" -> {
                    normalized.put("kind", "text");
                    normalized.put("text", part.path("text").asText(""));
                }
                case "data" -> {
                    normalized.put("kind", "text");
                    normalized.put("text", part.path("data").toString());
                }
                case "file" -> {
                    normalized.put("kind", "text");
                    JsonNode file = part.path("file");
                    normalized.put("text", "[file] " + file.path("name").asText(file.path("uri").asText("attachment")));
                }
                default -> {
                    normalized.put("kind", "text");
                    normalized.put("text", part.toString());
                }
            }
            parts.add(normalized);
        }
        if (parts.isEmpty()) {
            parts.add(Map.of("kind", "text", "text", ""));
        }
        return parts;
    }

    /** 从 Message 中抽取纯文本输入（text parts 拼接） */
    private String extractText(JsonNode messageNode) {
        StringBuilder builder = new StringBuilder();
        for (JsonNode part : messageNode.path("parts")) {
            if (part.has("text")) {
                builder.append(part.path("text").asText());
            } else if (part.has("data")) {
                builder.append(part.path("data").toString());
            }
        }
        return builder.toString();
    }

    // ── 事件构造（与规范 JSON 形状一致） ─────────────

    private Map<String, Object> artifactUpdateEvent(A2aTask task, String delta, boolean append, boolean lastChunk) {
        Map<String, Object> artifact = new LinkedHashMap<>();
        artifact.put("artifactId", task.artifactId());
        artifact.put("name", "response");
        artifact.put("parts", List.of(Map.of("kind", "text", "text", delta)));
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("taskId", task.id());
        event.put("contextId", task.contextId());
        event.put("artifact", artifact);
        event.put("append", append);
        event.put("lastChunk", lastChunk);
        event.put("kind", "artifact-update");
        return event;
    }

    private Map<String, Object> statusUpdateEvent(A2aTask task, boolean finalFlag, String failureMessage) {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("state", task.state());
        status.put("timestamp", task.updatedAt());
        if (failureMessage != null && !failureMessage.isBlank()) {
            status.put("message", agentMessage(failureMessage));
        }
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("taskId", task.id());
        event.put("contextId", task.contextId());
        event.put("status", status);
        event.put("final", finalFlag);
        event.put("kind", "status-update");
        return event;
    }

    private Map<String, Object> toolStatusEvent(A2aTask task, AgentToolCallEventResponseDTO toolEvent) {
        boolean isResult = "result".equals(toolEvent.phase());
        String summary = (isResult ? "工具返回 " : "调用工具 ") + toolEvent.toolName();
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("state", "working");
        status.put("timestamp", Instant.now().toString());
        status.put("message", agentMessage(summary));
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("taskId", task.id());
        event.put("contextId", task.contextId());
        event.put("status", status);
        event.put("final", false);
        event.put("kind", "status-update");
        return event;
    }

    private Map<String, Object> failureStatusEvent(String taskId, String contextId, Exception exception) {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("state", "failed");
        status.put("timestamp", Instant.now().toString());
        status.put("message", agentMessage(exception.getMessage() == null ? "任务执行失败" : exception.getMessage()));
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("taskId", taskId);
        event.put("contextId", contextId);
        event.put("status", status);
        event.put("final", true);
        event.put("kind", "status-update");
        return event;
    }

    private Map<String, Object> agentMessage(String text) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "agent");
        message.put("parts", List.of(Map.of("kind", "text", "text", text)));
        message.put("messageId", "msg-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        message.put("kind", "message");
        return message;
    }

    // ── A2aTask：任务内存态 ─────────────────────────

    private final class A2aTask {
        private final String id;
        private final String contextId;
        private final String agentId;
        private final String artifactId;
        private final Map<String, Object> userMessage;
        private final StringBuilder text = new StringBuilder();
        private volatile String state = "working";
        private volatile String failureMessage;
        private volatile Instant updatedAt = Instant.now();

        private A2aTask(String id, String contextId, String agentId, Map<String, Object> userMessage) {
            this.id = id;
            this.contextId = contextId;
            this.agentId = agentId;
            this.artifactId = "art-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            this.userMessage = userMessage;
        }

        String id() { return id; }

        String contextId() { return contextId; }

        String agentId() { return agentId; }

        String artifactId() { return artifactId; }

        synchronized void appendText(String delta) {
            text.append(delta == null ? "" : delta);
            updatedAt = Instant.now();
        }

        synchronized void complete() {
            this.state = "completed";
            this.updatedAt = Instant.now();
        }

        synchronized void fail(String message) {
            this.state = "failed";
            this.failureMessage = message;
            this.updatedAt = Instant.now();
        }

        synchronized void cancel() {
            this.state = "canceled";
            this.updatedAt = Instant.now();
        }

        String state() {
            return state;
        }

        String updatedAt() {
            return updatedAt.toString();
        }

        boolean isTerminal() {
            return "completed".equals(state) || "failed".equals(state)
                    || "canceled".equals(state) || "rejected".equals(state);
        }

        /** 规范 Task 快照：kind/id/contextId/status/artifacts/history */
        synchronized Map<String, Object> snapshot() {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("kind", "task");
            snapshot.put("id", id);
            snapshot.put("contextId", contextId);
            Map<String, Object> status = new LinkedHashMap<>();
            status.put("state", state);
            status.put("timestamp", updatedAt.toString());
            if (failureMessage != null && !failureMessage.isBlank()) {
                status.put("message", agentMessage(failureMessage));
            }
            snapshot.put("status", status);
            String artifactText = text.toString();
            if (!artifactText.isBlank()) {
                snapshot.put("artifacts", List.of(Map.of(
                        "artifactId", artifactId,
                        "name", "response",
                        "parts", List.of(Map.of("kind", "text", "text", artifactText)))));
            } else {
                snapshot.put("artifacts", List.of());
            }
            List<Map<String, Object>> history = new ArrayList<>();
            history.add(userMessage);
            if (!artifactText.isBlank()) {
                history.add(agentMessage(artifactText));
            }
            snapshot.put("history", history);
            return snapshot;
        }
    }

    // ── JSON-RPC 信封 ───────────────────────────────

    private JsonNode idOf(JsonNode request) {
        if (request == null || request.isMissingNode() || !request.hasNonNull("id")) {
            return null;
        }
        return request.path("id");
    }

    private Map<String, Object> resultResponse(JsonNode id, Object result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("result", result);
        return response;
    }

    private Map<String, Object> errorResponse(JsonNode id, int code, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("error", Map.of("code", code, "message", message));
        return response;
    }

    /** SSE 单条推送：每条 data 均为完整 JSON-RPC 响应信封 */
    private void sendResult(SseEmitter emitter, AtomicBoolean closed, JsonNode id, Object result) {
        if (closed.get()) {
            return;
        }
        try {
            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(resultResponse(id, result))));
        } catch (IOException exception) {
            closed.set(true);
        }
    }
}
