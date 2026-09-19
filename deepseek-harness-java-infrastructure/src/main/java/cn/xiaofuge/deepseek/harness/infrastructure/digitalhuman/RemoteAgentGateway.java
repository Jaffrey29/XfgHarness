package cn.xiaofuge.deepseek.harness.infrastructure.digitalhuman;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 远端 Agent 网关：把任务转发给远端（或本机回环）DSH 的 /api/agent/stream，
 * 解析其 SSE 事件流并按统一语义回调（chunk/reasoning/tool/finish/error）。
 *
 * <p>只负责协议转换与事件抽取，协作语义（任务状态、事件落库）由
 * CollaborationService 承载。</p>
 */
public class RemoteAgentGateway {

    private static final Logger log = LoggerFactory.getLogger(RemoteAgentGateway.class);
    /**
     * 流空闲判定阈值。上游 SseEmitter 无心跳，工具执行期间（shell 可能跑数分钟）完全没有
     * SSE 字节是常态，阈值必须显著大于常见工具耗时；此前 30s 会在长工具执行中途掐断流，
     * 上游随即 Broken pipe，任务被当成"正常结束"收口成半截结果。
     */
    private static final long STREAM_IDLE_CLOSE_MS = 300_000L;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    /** taskKey（taskId）-> 取消标志：cancel() 写入，流读循环逐行检查，命中即中断执行 */
    private final Set<String> cancelled = ConcurrentHashMap.newKeySet();

    public RemoteAgentGateway() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /** 执行流事件回调 */
    public interface EventSink {
        void onChunk(String text);

        void onReasoning(String text);

        void onToolCall(String toolName, String summary);

        /**
         * 携带 callId 的工具调用回调：上游（Agent SSE step_break / tool_result）都带 callId，
         * 协作层用它去重（同一调用双发）与配对（调用 ↔ 结果）。默认实现退化为无 callId 版本。
         */
        default void onToolCall(String toolName, String summary, String callId) {
            onToolCall(toolName, summary);
        }

        void onToolResult(String toolName, String summary);

        default void onToolResult(String toolName, String summary, String rawResult) {
            onToolResult(toolName, summary);
        }

        default void onToolResult(String toolName, String summary, String rawResult, String callId) {
            onToolResult(toolName, summary, rawResult);
        }

        /** 执行在审批点中断：携带审批关键信息（若流内有），由协作层落房间事件 */
        default void onApprovalRequired(String toolName, String summary) {
        }
    }

    /** 执行在审批点中断（Agent 返回的流正常结束但任务实际等待审批） */
    public static class ApprovalInterruptException extends Exception {
        public ApprovalInterruptException(String message) {
            super(message);
        }
    }

    /** 远端数字人：POST {baseUrl}/api/agent/stream */
    public void runRemote(String baseUrl, String token, String instruction,
                          String channelCode, String approvalMode, EventSink sink) throws Exception {
        runRemote(null, baseUrl, token, instruction, channelCode, approvalMode, sink);
    }

    /** 带 taskKey 的重载：taskKey 命中取消标志时立即中断流读取。 */
    public void runRemote(String taskKey, String baseUrl, String token, String instruction,
                          String channelCode, String approvalMode, EventSink sink) throws Exception {
        String url = baseUrl.replaceAll("/+$", "") + "/api/agent/stream";
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(buildBody(instruction, null, approvalMode, null, null)));
        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Bearer " + token.trim());
        }
        stream(builder.build(), sink, taskKey);
    }

    /**
     * A2A 数字人调用链：
     * <ol>
     *   <li>标准 A2A 发现：读 Agent Card 的 url 字段（即 JSON-RPC 端点，如 {base}/a2a）；
     *       发现失败时回退猜测 {base}/a2a。</li>
     *   <li>JSON-RPC {@code message/stream}（SSE 流式，A2A 0.3.x）。</li>
     *   <li>JSON-RPC {@code message/send}（阻塞式）。</li>
     *   <li>兼容旧版：REST 路径 {base}/message/stream、{base}/message/send（A2A 0.2.x 风格）。</li>
     * </ol>
     * 注意：DSH 的 JSON-RPC 端点在 {base}/a2a（见 AgentCardController.url），
     * message/send、message/stream 是 JSON-RPC method 而非 URL 路径。
     */
    public void runA2a(String baseUrl, String token, String instruction, EventSink sink) throws Exception {
        runA2a(null, baseUrl, token, instruction, sink);
    }

    /** 带 taskKey 的重载：支持协作层取消 + HTTP 表面错误快速失败。 */
    public void runA2a(String taskKey, String baseUrl, String token, String instruction, EventSink sink) throws Exception {
        String normalized = baseUrl.replaceAll("/+$", "");
        Exception lastError = null;

        String jsonRpcUrl = discoverA2aUrl(normalized, token);
        if (jsonRpcUrl == null || jsonRpcUrl.isBlank()) {
            jsonRpcUrl = normalized + "/a2a";
        }
        log.info("A2A JSON-RPC endpoint: {}", jsonRpcUrl);

        // 回退策略：
        // - 传输层失败（连不上/超时）：逐级回退（下一端点可能可达）
        // - HTTP 表面错误（有响应但 4xx/5xx）：同一服务器，仅当 404/405/501（路由/方法不存在）时
        //   允许换 method/path 再试一次；500/502/503/504 直接失败——远端已明确报错，傻等回退链只会拖长卡死时间
        try {
            ensureNotCancelled(taskKey);
            streamA2a(a2aJsonRpcRequest(jsonRpcUrl, token, "message/stream", instruction)
                    .timeout(Duration.ofMinutes(10))
                    .build(), sink, taskKey);
            return;
        } catch (Exception error) {
            lastError = error;
            if (abortFallback(taskKey, error)) throw error;
            log.debug("A2A message/stream failed, fallback: {}", error.getMessage());
        }

        try {
            ensureNotCancelled(taskKey);
            sendA2a(a2aJsonRpcRequest(jsonRpcUrl, token, "message/send", instruction)
                    .timeout(Duration.ofMinutes(10))
                    .build(), sink);
            return;
        } catch (Exception error) {
            lastError = error;
            if (abortFallback(taskKey, error)) throw error;
            log.debug("A2A message/send failed, fallback: {}", error.getMessage());
        }

        try {
            ensureNotCancelled(taskKey);
            streamA2a(a2aMessageRequest(normalized + "/message/stream", token, instruction)
                    .timeout(Duration.ofMinutes(10))
                    .build(), sink, taskKey);
            return;
        } catch (Exception error) {
            lastError = error;
            if (abortFallback(taskKey, error)) throw error;
            log.debug("A2A legacy /message/stream failed, fallback: {}", error.getMessage());
        }

        try {
            ensureNotCancelled(taskKey);
            sendA2a(a2aMessageRequest(normalized + "/message/send", token, instruction)
                    .timeout(Duration.ofMinutes(10))
                    .build(), sink);
            return;
        } catch (Exception error) {
            lastError = error;
        }
        throw lastError == null ? new IllegalStateException("A2A Agent 调用失败") : lastError;
    }

    /**
     * 是否应终止回退链：任务已取消，或收到不可路由修复的 HTTP 表面错误
     * （500/502/503/504 等——远端服务器活着且明确报错，换端点无意义）。
     */
    private boolean abortFallback(String taskKey, Exception error) {
        if (isCancelled(taskKey)) return true;
        if (error instanceof HttpSurfaceException surface) {
            // 404/405/501 可能只是端点路径/方法不对，允许下一级回退
            return surface.status != 404 && surface.status != 405 && surface.status != 501;
        }
        return false;
    }

    /** HTTP 有响应但状态码非 2xx：与传输层失败（连不上/超时）区分开，用于快速失败决策。 */
    static class HttpSurfaceException extends IllegalStateException {
        final int status;

        HttpSurfaceException(int status, String message) {
            super(message);
            this.status = status;
        }
    }

    private void sendA2a(HttpRequest request, EventSink sink) throws Exception {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new HttpSurfaceException(response.statusCode(),
                    "A2A Agent 返回 HTTP " + response.statusCode() + "：" + abbreviate(response.body()));
        }
        JsonNode payload = objectMapper.readTree(response.body());
        String text = extractA2aText(payload);
        if (!text.isBlank()) sink.onChunk(text);
    }

    private HttpRequest.Builder a2aMessageRequest(String url, String token, String instruction) throws Exception {
        Map<String, Object> body = Map.of(
                "message", Map.of(
                        "role", "user",
                        "parts", java.util.List.of(Map.of("kind", "text", "text", instruction))
                )
        );
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Bearer " + token.trim());
        }
        return builder;
    }

    private HttpRequest.Builder a2aJsonRpcRequest(String url, String token, String method, String instruction) throws Exception {
        Map<String, Object> body = Map.of(
                "jsonrpc", "2.0",
                "id", "a2a-" + System.currentTimeMillis(),
                "method", method,
                "params", Map.of(
                        "message", Map.of(
                                "role", "user",
                                "parts", java.util.List.of(Map.of("kind", "text", "text", instruction)),
                                "messageId", "msg-" + System.currentTimeMillis(),
                                "kind", "message"
                        )
                )
        );
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Bearer " + token.trim());
        }
        return builder;
    }

    private void streamA2a(HttpRequest request, EventSink sink) throws Exception {
        streamA2a(request, sink, null);
    }

    private void streamA2a(HttpRequest request, EventSink sink, String taskKey) throws Exception {
        HttpResponse<java.io.InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            String errorBody;
            try (var stream = response.body()) {
                errorBody = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
            throw new HttpSurfaceException(response.statusCode(),
                    "A2A Agent 返回 HTTP " + response.statusCode() + "：" + abbreviate(errorBody));
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            StringBuilder dataLines = new StringBuilder();
            String line;
            long lastEventAt = System.currentTimeMillis();
            while ((line = readLineWithIdleClose(reader, taskKey, lastEventAt)) != null) {
                ensureNotCancelled(taskKey);
                if (line.startsWith("data:")) {
                    String value = line.substring(5);
                    if (value.startsWith(" ")) value = value.substring(1);
                    if (dataLines.length() > 0) dataLines.append("\n");
                    dataLines.append(value);
                } else if (line.isBlank()) {
                    if (dataLines.length() > 0) {
                        boolean terminal = dispatchA2a(dataLines.toString(), sink);
                        dataLines.setLength(0);
                        lastEventAt = System.currentTimeMillis();
                        if (terminal) return;
                    }
                }
            }
            if (dataLines.length() > 0) {
                dispatchA2a(dataLines.toString(), sink);
            }
        }
    }

    private boolean dispatchA2a(String raw, EventSink sink) {
        try {
            JsonNode payload = objectMapper.readTree(raw);
            // JSON-RPC error 响应：视为失败，向上抛出以触发回退链
            JsonNode error = payload.path("error");
            if (!error.isMissingNode() && !error.isNull() && error.isObject()) {
                throw new IllegalStateException("A2A Agent 错误 "
                        + error.path("code").asText("") + "：" + error.path("message").asText(""));
            }
            JsonNode result = payload.path("result");
            if (result.isObject() && "status-update".equals(result.path("kind").asText())) {
                String state = result.path("status").path("state").asText("");
                if ("failed".equals(state) || "rejected".equals(state) || "canceled".equals(state)) {
                    String detail = result.path("status").path("message").path("parts")
                            .path(0).path("text").asText("");
                    throw new IllegalStateException("A2A 任务"
                            + ("canceled".equals(state) ? "已取消" : "失败") + "：" + detail);
                }
                // completed 是 A2A 的终态；即使连接未关闭也应让协作任务收口
                return "completed".equals(state);
            }
            String text = extractA2aText(payload);
            if (!text.isBlank()) sink.onChunk(text);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            if (!raw.isBlank()) sink.onChunk(raw);
        }
        return false;
    }

    /** 标准 A2A 发现：读取 Agent Card 的 url 字段（JSON-RPC 端点）；不可用返回 null。 */
    private String discoverA2aUrl(String normalized, String token) {
        for (String path : new String[]{"/.well-known/agent-card.json", "/.well-known/agent.json"}) {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(normalized + path))
                        .timeout(Duration.ofSeconds(8))
                        .GET();
                if (token != null && !token.isBlank()) {
                    builder.header("Authorization", "Bearer " + token.trim());
                }
                HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    JsonNode url = objectMapper.readTree(response.body()).path("url");
                    if (url.isTextual() && !url.asText().isBlank()) {
                        String endpoint = url.asText().replaceAll("/+$", "");
                        // 相对路径端点补全为绝对 URL
                        if (endpoint.startsWith("/")) {
                            endpoint = normalized + endpoint;
                        }
                        return endpoint;
                    }
                }
            } catch (Exception e) {
                log.debug("A2A card discover failed via {}: {}", path, e.getMessage());
            }
        }
        return null;
    }

    public void runLocal(String instruction, String channelCode, String approvalMode, EventSink sink) throws Exception {
        runLocal(instruction, channelCode, approvalMode, sink, null, null);
    }

    /**
     * 本地数字人：回环调用本服务的 /api/agent/stream。
     * 通过运行时实际端口自连，避免在协作层重复实现 Agent 主循环。
     *
     * @param cwd          数字人执行时的工作目录（会话归属项目路径），空则用进程目录
     * @param sandboxRoots 用户授权的额外可写根（@ 工程 + 资源目录），workspace-write 沙箱据此放行
     */
    public void runLocal(String instruction, String channelCode, String approvalMode, EventSink sink,
                         String cwd, java.util.List<String> sandboxRoots) throws Exception {
        runLocal(null, instruction, channelCode, approvalMode, sink, cwd, sandboxRoots);
    }

    /** 带 taskKey 的重载：支持协作层取消。 */
    public void runLocal(String taskKey, String instruction, String channelCode, String approvalMode, EventSink sink,
                         String cwd, java.util.List<String> sandboxRoots) throws Exception {
        String port = System.getProperty("server.port", "8080");
        String url = "http://127.0.0.1:" + port + "/api/agent/stream";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(buildBody(instruction, channelCode, approvalMode, cwd, sandboxRoots)))
                .build();
        stream(request, sink, taskKey);
    }

    /** 标记取消：流读循环逐行检查该标志，命中即抛 CancellationException 中断执行。 */
    public void cancel(String taskKey) {
        if (taskKey != null) cancelled.add(taskKey);
    }

    /** 任务（重试）启动前清除取消标志，避免历史取消残留阻断新执行。 */
    public void clearCancelled(String taskKey) {
        if (taskKey != null) cancelled.remove(taskKey);
    }

    private boolean isCancelled(String taskKey) {
        return taskKey != null && cancelled.contains(taskKey);
    }

    private void ensureNotCancelled(String taskKey) {
        if (isCancelled(taskKey)) {
            throw new java.util.concurrent.CancellationException("任务已被取消");
        }
    }

    private String buildBody(String instruction, String channelCode, String approvalMode,
                             String cwd, java.util.List<String> sandboxRoots) throws Exception {
        var body = new java.util.LinkedHashMap<String, Object>();
        body.put("agentId", "collab-" + System.currentTimeMillis());
        body.put("message", instruction);
        if (cwd != null && !cwd.isBlank()) {
            body.put("cwd", cwd);
        }
        if (sandboxRoots != null && !sandboxRoots.isEmpty()) {
            body.put("sandboxRoots", sandboxRoots);
        }
        if (channelCode != null && !channelCode.isBlank()) {
            body.put("channelCode", channelCode);
        }
        body.put("approvalMode", approvalMode == null || approvalMode.isBlank() ? "AUTO_APPROVE" : approvalMode);
        return objectMapper.writeValueAsString(body);
    }

    /** 消费 DSH SSE 流：event: xxx + data: {json}，空行分隔 */
    private void stream(HttpRequest request, EventSink sink) throws Exception {
        stream(request, sink, null);
    }

    private void stream(HttpRequest request, EventSink sink, String taskKey) throws Exception {
        HttpResponse<java.io.InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            String errorBody;
            try (var stream = response.body()) {
                errorBody = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
            throw new HttpSurfaceException(response.statusCode(),
                    "Agent 服务返回 HTTP " + response.statusCode() + "：" + abbreviate(errorBody));
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String eventName = "";
            StringBuilder dataLines = new StringBuilder();
            String line;
            long lastEventAt = System.currentTimeMillis();
            while ((line = readLineWithIdleClose(reader, taskKey, lastEventAt)) != null) {
                ensureNotCancelled(taskKey);
                if (line.startsWith("event:")) {
                    eventName = line.substring(6).trim();
                } else if (line.startsWith("data:")) {
                    String value = line.substring(5);
                    if (value.startsWith(" ")) value = value.substring(1);
                    if (dataLines.length() > 0) dataLines.append("\n");
                    dataLines.append(value);
                } else if (line.isBlank()) {
                    if (dataLines.length() > 0) {
                        boolean terminal = dispatch(eventName, dataLines.toString(), sink);
                        dataLines.setLength(0);
                        eventName = "";
                        lastEventAt = System.currentTimeMillis();
                        if (terminal) return;
                    } else if (isTerminalEvent(eventName)) {
                        return;
                    }
                }
            }
            if (dataLines.length() > 0) {
                dispatch(eventName, dataLines.toString(), sink);
            }
        }
    }

    private String readLineWithIdleClose(BufferedReader reader, String taskKey, long lastEventAt) throws Exception {
        while (!reader.ready()) {
            ensureNotCancelled(taskKey);
            if (System.currentTimeMillis() - lastEventAt >= STREAM_IDLE_CLOSE_MS) {
                // 空闲超时必须抛错而不是静默返回：返回 null 会被调用方当作正常 EOF，
                // 协作层会把半截结果标成 COMPLETED。抛异常让 runTask 走自动恢复（改派/重试）。
                throw new IllegalStateException("Agent 流空闲超过 " + (STREAM_IDLE_CLOSE_MS / 1000)
                        + "s 无事件，网关主动断开（taskKey=" + taskKey + "）");
            }
            Thread.sleep(200L);
        }
        return reader.readLine();
    }

    private boolean dispatch(String eventName, String raw, EventSink sink) {
        String normalizedEventName = eventName == null || eventName.isBlank() ? "chunk" : eventName;
        if (isTerminalEvent(normalizedEventName)) {
            // 完成信号由调用方收尾，无需转发；不依赖 data 是否为 JSON
            return true;
        }
        try {
            JsonNode payload = objectMapper.readTree(raw);
            switch (normalizedEventName) {
                case "chunk", "message.delta" -> sink.onChunk(extractText(payload));
                case "reasoning", "message.reasoning" -> sink.onReasoning(extractText(payload));
                case "tool_result", "tool.result" -> {
                    String toolName = payload.path("toolName").asText(
                            payload.path("name").asText("tool"));
                    String callId = payload.path("callId").asText("");
                    JsonNode resultNode = payload.path("result");
                    String rawResult = resultNode.isMissingNode() ? "" : rawJson(resultNode);
                    String summary = abbreviate(resultNode.isMissingNode() ? extractText(payload) : extractText(resultNode));
                    // tool_result 事件在 DSH 中既承载发起也承载返回，按 status 区分
                    String status = payload.path("status").asText("");
                    if ("running".equals(status) || "call".equals(status)) {
                        sink.onToolCall(toolName, summary, callId);
                    } else {
                        sink.onToolResult(toolName, summary, rawResult, callId);
                    }
                }
                case "step_break", "tool.call" -> {
                    String toolName = payload.path("toolName").asText("tool");
                    String callId = payload.path("callId").asText("");
                    String status = payload.path("status").asText("");
                    String summary = abbreviate(payload.toString());
                    // 审批中断信号：工具调用进入审批等待（发起后无对应 result 即被挂起）
                    if (status.contains("approval") || status.contains("pending")
                            || "waiting".equals(status) || "requires_approval".equals(status)) {
                        sink.onApprovalRequired(toolName, summary);
                    } else {
                        sink.onToolCall(toolName, summary, callId);
                    }
                }
                case "error" -> throw new IllegalStateException(
                        payload.path("message").asText(extractText(payload)));
                default -> {
                    // meta 等事件忽略
                }
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            // 非 JSON 数据按纯文本 chunk 处理
            if ("chunk".equals(eventName) || eventName == null || eventName.isBlank()) {
                sink.onChunk(raw);
            } else {
                log.debug("Skip unparsable SSE event {}: {}", eventName, e.getMessage());
            }
        }
        return false;
    }

    private boolean isTerminalEvent(String eventName) {
        // finish 后服务端还会立即发 done；在 finish 就断开会让上游记录 Broken pipe。
        // done 才是完整终态；如果连接异常挂住，另有空闲兜底。
        return "done".equals(eventName);
    }

    private String extractText(JsonNode payload) {
        if (payload.isTextual()) return payload.asText();
        JsonNode nestedPayload = payload.path("payload");
        if (!nestedPayload.isMissingNode() && !nestedPayload.isNull()) {
            String nestedText = extractText(nestedPayload);
            if (!nestedText.isBlank()) return nestedText;
        }
        for (String field : new String[]{"text", "content", "delta", "message", "result"}) {
            JsonNode node = payload.path(field);
            if (node.isTextual()) return node.asText();
        }
        return payload.toString();
    }

    private String extractA2aText(JsonNode payload) {
        if (payload == null || payload.isMissingNode() || payload.isNull()) return "";
        if (payload.isTextual()) return payload.asText();
        JsonNode result = payload.path("result");
        if (!result.isMissingNode()) {
            String text = extractA2aText(result);
            if (!text.isBlank()) return text;
        }
        JsonNode message = payload.path("message");
        if (!message.isMissingNode()) {
            String text = extractA2aText(message);
            if (!text.isBlank()) return text;
        }
        JsonNode artifact = payload.path("artifact");
        if (!artifact.isMissingNode()) {
            String text = extractA2aText(artifact);
            if (!text.isBlank()) return text;
        }
        JsonNode parts = payload.path("parts");
        if (parts.isArray()) {
            StringBuilder builder = new StringBuilder();
            parts.forEach(part -> {
                String text = part.path("text").asText("");
                if (!text.isBlank()) builder.append(text);
            });
            if (builder.length() > 0) return builder.toString();
        }
        for (String field : new String[]{"text", "content", "delta", "message", "result"}) {
            JsonNode node = payload.path(field);
            if (node.isTextual()) return node.asText();
        }
        return "";
    }

    private String rawJson(JsonNode payload) {
        if (payload == null || payload.isMissingNode() || payload.isNull()) return "";
        return payload.isTextual() ? payload.asText() : payload.toString();
    }

    private String abbreviate(String text) {
        if (text == null) return "";
        String singleLine = text.replaceAll("\\s+", " ").trim();
        return singleLine.length() > 200 ? singleLine.substring(0, 200) + "…" : singleLine;
    }
}
