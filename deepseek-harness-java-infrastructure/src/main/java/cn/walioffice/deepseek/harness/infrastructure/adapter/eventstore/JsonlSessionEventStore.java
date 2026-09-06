package cn.walioffice.deepseek.harness.infrastructure.adapter.eventstore;

import cn.walioffice.deepseek.harness.domain.session.event.adapter.repository.ISessionEventStore;
import cn.walioffice.deepseek.harness.domain.session.event.model.entity.SessionEvent;
import cn.walioffice.deepseek.harness.domain.session.event.model.entity.SessionHeader;
import cn.walioffice.deepseek.harness.domain.session.event.model.valobj.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * JSONL file-based implementation of {@link ISessionEventStore}.
 * <p>
 * Each session is persisted as two files under the base directory:
 * <ul>
 *   <li>{@code <sessionId>/header.json} — session header</li>
 *   <li>{@code <sessionId>/events.jsonl} — append-only event log (one JSON per line)</li>
 * </ul>
 * Mirrors TS {@code session-persistence-jsonl}.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class JsonlSessionEventStore implements ISessionEventStore {

    private final Path baseDir;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, Object> fileLocks = new ConcurrentHashMap<>();

    public JsonlSessionEventStore(String baseDir) {
        this.baseDir = Paths.get(baseDir).toAbsolutePath().normalize();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }

    @Override
    public void saveHeader(SessionHeader header) {
        try {
            Path dir = sessionDir(header.sessionId());
            Files.createDirectories(dir);
            Path headerFile = dir.resolve("header.json");
            ObjectNode node = objectMapper.createObjectNode();
            node.put("version", header.version());
            node.put("sessionId", header.sessionId());
            node.put("createdAt", header.createdAt().toString());
            node.put("cwd", header.cwd());
            if (header.parentSession() != null) node.put("parentSession", header.parentSession());
            if (header.seedLength() >= 0) node.put("seedLength", header.seedLength());
            if (header.origin() != null) node.put("origin", header.origin());
            node.put("delegationDepth", header.delegationDepth());
            if (header.agentPreset() != null) node.put("agentPreset", header.agentPreset());
            Files.writeString(headerFile, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save header for session " + header.sessionId(), e);
        }
    }

    @Override
    public Optional<SessionHeader> getHeader(String sessionId) {
        Path headerFile = sessionDir(sessionId).resolve("header.json");
        if (!Files.exists(headerFile)) {
            return Optional.empty();
        }
        try {
            var node = objectMapper.readTree(Files.readString(headerFile, StandardCharsets.UTF_8));
            return Optional.of(new SessionHeader(
                    node.has("version") ? node.get("version").asInt() : 1,
                    node.get("sessionId").asText(),
                    Instant.parse(node.get("createdAt").asText()),
                    node.has("cwd") ? node.get("cwd").asText() : null,
                    node.has("parentSession") ? node.get("parentSession").asText() : null,
                    node.has("seedLength") ? node.get("seedLength").asInt() : -1,
                    node.has("origin") ? node.get("origin").asText() : null,
                    node.has("delegationDepth") ? node.get("delegationDepth").asInt() : 0,
                    node.has("agentPreset") ? node.get("agentPreset").asText() : null
            ));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read header for session " + sessionId, e);
        }
    }

    @Override
    public void append(String sessionId, SessionEvent event) {
        Object lock = fileLocks.computeIfAbsent(sessionId, k -> new Object());
        synchronized (lock) {
            try {
                Path eventsFile = sessionDir(sessionId).resolve("events.jsonl");
                Files.createDirectories(eventsFile.getParent());
                String json = serializeEvent(sessionId, event);
                Files.writeString(eventsFile, json + "\n", StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new RuntimeException("Failed to append event to session " + sessionId, e);
            }
        }
    }

    @Override
    public void appendBatch(String sessionId, List<SessionEvent> events) {
        Object lock = fileLocks.computeIfAbsent(sessionId, k -> new Object());
        synchronized (lock) {
            try {
                Path eventsFile = sessionDir(sessionId).resolve("events.jsonl");
                Files.createDirectories(eventsFile.getParent());
                StringBuilder sb = new StringBuilder();
                for (SessionEvent event : events) {
                    sb.append(serializeEvent(sessionId, event)).append("\n");
                }
                Files.writeString(eventsFile, sb.toString(), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new RuntimeException("Failed to append batch to session " + sessionId, e);
            }
        }
    }

    @Override
    public List<SessionEvent> readAll(String sessionId) {
        return readEvents(sessionId, 0, Long.MAX_VALUE);
    }

    @Override
    public List<SessionEvent> readAfter(String sessionId, long afterSeq) {
        return readEvents(sessionId, afterSeq + 1, Long.MAX_VALUE);
    }

    @Override
    public List<SessionEvent> readRange(String sessionId, long fromSeq, long toSeq) {
        return readEvents(sessionId, fromSeq, toSeq);
    }

    @Override
    public long getLastSeq(String sessionId) {
        Path eventsFile = sessionDir(sessionId).resolve("events.jsonl");
        if (!Files.exists(eventsFile)) {
            return -1;
        }
        long lastSeq = -1;
        try {
            for (String line : Files.readAllLines(eventsFile, StandardCharsets.UTF_8)) {
                if (line.isBlank()) continue;
                var node = objectMapper.readTree(line);
                long seq = node.get("seq").asLong();
                if (seq > lastSeq) lastSeq = seq;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read last seq for session " + sessionId, e);
        }
        return lastSeq;
    }

    @Override
    public long count(String sessionId) {
        Path eventsFile = sessionDir(sessionId).resolve("events.jsonl");
        if (!Files.exists(eventsFile)) {
            return 0;
        }
        try {
            return Files.lines(eventsFile, StandardCharsets.UTF_8)
                    .filter(line -> !line.isBlank())
                    .count();
        } catch (IOException e) {
            throw new RuntimeException("Failed to count events for session " + sessionId, e);
        }
    }

    @Override
    public List<SessionHeader> listHeaders(int limit) {
        if (!Files.isDirectory(baseDir)) {
            return List.of();
        }
        try (var stream = Files.list(baseDir)) {
            return stream
                    .filter(Files::isDirectory)
                    .map(dir -> {
                        try {
                            return getHeader(dir.getFileName().toString());
                        } catch (Exception e) {
                            return java.util.Optional.<SessionHeader>empty();
                        }
                    })
                    .filter(java.util.Optional::isPresent)
                    .map(java.util.Optional::get)
                    .sorted((a, b) -> b.createdAt().compareTo(a.createdAt()))
                    .limit(limit)
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Failed to list session headers", e);
        }
    }

    // ── Internal helpers ─────────────────────────────────────────

    private List<SessionEvent> readEvents(String sessionId, long fromSeq, long toSeq) {
        Path eventsFile = sessionDir(sessionId).resolve("events.jsonl");
        if (!Files.exists(eventsFile)) {
            return List.of();
        }
        List<SessionEvent> events = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(eventsFile, StandardCharsets.UTF_8)) {
                if (line.isBlank()) continue;
                var node = objectMapper.readTree(line);
                long seq = node.get("seq").asLong();
                if (seq < fromSeq || seq > toSeq) continue;
                events.add(deserializeEvent(node));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read events for session " + sessionId, e);
        }
        return events;
    }

    private String serializeEvent(String sessionId, SessionEvent event) {
        try {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("seq", event.seq());
            node.put("type", event.type().wireName());
            node.put("time", event.time().toString());

            // Payload serialization
            String payloadJson = serializePayload(event);
            if (payloadJson != null && !payloadJson.isBlank()) {
                node.set("payload", objectMapper.readTree(payloadJson));
            }

            // Surface intent
            SurfaceIntent intent = event.surfaceIntent();
            if (intent != null && intent.surfaceOp() != null) {
                if (intent.surfaceOp() instanceof SurfaceOp.Append) {
                    node.put("surfaceOp", "append");
                } else if (intent.surfaceOp() instanceof SurfaceOp.Replace r) {
                    node.put("surfaceOp", "replace:" + r.start() + ":" + r.end());
                }
                if (intent.sourceEventSeqs() != null && !intent.sourceEventSeqs().isEmpty()) {
                    node.put("sourceEventSeqs", intent.sourceEventSeqs().stream()
                            .map(String::valueOf).collect(Collectors.joining(",")));
                }
            }

            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }

    private SessionEvent deserializeEvent(com.fasterxml.jackson.databind.JsonNode node) {
        long seq = node.get("seq").asLong();
        Instant time = Instant.parse(node.get("time").asText());
        SessionEventType type = SessionEventType.fromWireName(node.get("type").asText());

        String payloadJson = node.has("payload") ? node.get("payload").toString() : null;
        Object payload = deserializePayload(type, payloadJson);

        SurfaceIntent surfaceIntent = null;
        if (node.has("surfaceOp")) {
            String surfaceOpStr = node.get("surfaceOp").asText();
            SurfaceOp op;
            if ("append".equals(surfaceOpStr)) {
                op = new SurfaceOp.Append();
            } else if (surfaceOpStr.startsWith("replace:")) {
                String[] parts = surfaceOpStr.split(":");
                op = new SurfaceOp.Replace(Long.parseLong(parts[1]), Long.parseLong(parts[2]));
            } else {
                op = null;
            }
            List<Long> seqs = null;
            if (node.has("sourceEventSeqs")) {
                seqs = java.util.Arrays.stream(node.get("sourceEventSeqs").asText().split(","))
                        .map(Long::parseLong).toList();
            }
            if (op != null) surfaceIntent = new SurfaceIntent(op, seqs);
        }

        return switch (type) {
            case TURN_START -> new SessionEvent.TurnStart(seq, time);
            case TURN_END -> {
                TurnEndReason reason = deserializeTurnEndReason(payloadJson);
                yield new SessionEvent.TurnEnd(seq, time, reason);
            }
            case STEP_START -> {
                StepStartPayload p = (StepStartPayload) payload;
                yield new SessionEvent.StepStart(seq, time, p.turn(), p.step());
            }
            case STEP_END -> {
                StepEndPayload p = (StepEndPayload) payload;
                yield new SessionEvent.StepEnd(seq, time, p.turn(), p.step());
            }
            case USER_MESSAGE -> new SessionEvent.UserMessage(seq, time, (UserMessagePayload) payload, surfaceIntent);
            case ASSISTANT_CHUNK -> new SessionEvent.AssistantChunk(seq, time, (AssistantChunkPayload) payload);
            case ASSISTANT_MESSAGE -> new SessionEvent.AssistantMessage(seq, time, (AssistantMessagePayload) payload, surfaceIntent);
            case TOOL_CALL -> new SessionEvent.ToolCall(seq, time, (ToolCallPayload) payload);
            case TOOL_RESULT -> new SessionEvent.ToolResult(seq, time, (ToolResultPayload) payload, surfaceIntent);
            case TODO_WRITE -> new SessionEvent.TodoWrite(seq, time, (TodoWritePayload) payload);
            case REQUEST_HEADER -> new SessionEvent.RequestHeader(seq, time, (RequestHeaderPayload) payload);
            case REQUEST_CONTEXT -> new SessionEvent.RequestContext(seq, time, (RequestContextPayload) payload);
            case SESSION_END_SEED -> new SessionEvent.SessionEndSeed(seq, time);
            case AGENT_INBOX_SPLICED -> new SessionEvent.AgentInboxSpliced(seq, time, (InboxSplicedPayload) payload);
            case PLAN_MODE_CHANGE -> new SessionEvent.PlanModeChange(seq, time, (PlanModePayload) payload);
            case TASK_SUBMITTED, TASK_AUTO_APPROVED, TASK_APPROVAL_REQUIRED,
                 TASK_APPROVED, TASK_EXECUTION_STARTED,
                 TASK_EXECUTION_COMPLETED, TASK_EXECUTION_FAILED ->
                new SessionEvent.Generic(seq, time, type, payloadJson);
        };
    }

    private String serializePayload(SessionEvent event) {
        try {
            if (event instanceof SessionEvent.TurnStart) return "{}";
            if (event instanceof SessionEvent.TurnEnd e) {
                return objectMapper.writeValueAsString(java.util.Map.of("reason", turnEndReasonToJson(e.reason())));
            }
            if (event instanceof SessionEvent.StepStart e) {
                return objectMapper.writeValueAsString(java.util.Map.of("turn", e.turn(), "step", e.step()));
            }
            if (event instanceof SessionEvent.StepEnd e) {
                return objectMapper.writeValueAsString(java.util.Map.of("turn", e.turn(), "step", e.step()));
            }
            if (event instanceof SessionEvent.Generic e) {
                return e.payload() == null ? "" : e.payload();
            }
            // For events with data() method
            try {
                var dataMethod = event.getClass().getMethod("data");
                Object data = dataMethod.invoke(event);
                if (data != null) {
                    return objectMapper.writeValueAsString(data);
                }
            } catch (NoSuchMethodException ignored) {}
            return "{}";
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize payload", e);
        }
    }

    private Object deserializePayload(SessionEventType type, String json) {
        if (json == null || json.isBlank() || json.equals("{}")) return null;
        try {
            return switch (type) {
                case STEP_START -> objectMapper.readValue(json, StepStartPayload.class);
                case STEP_END -> objectMapper.readValue(json, StepEndPayload.class);
                case USER_MESSAGE -> objectMapper.readValue(json, UserMessagePayload.class);
                case ASSISTANT_CHUNK -> objectMapper.readValue(json, AssistantChunkPayload.class);
                case ASSISTANT_MESSAGE -> objectMapper.readValue(json, AssistantMessagePayload.class);
                case TOOL_CALL -> objectMapper.readValue(json, ToolCallPayload.class);
                case TOOL_RESULT -> objectMapper.readValue(json, ToolResultPayload.class);
                case TODO_WRITE -> objectMapper.readValue(json, TodoWritePayload.class);
                case REQUEST_HEADER -> objectMapper.readValue(json, RequestHeaderPayload.class);
                case REQUEST_CONTEXT -> objectMapper.readValue(json, RequestContextPayload.class);
                case AGENT_INBOX_SPLICED -> objectMapper.readValue(json, InboxSplicedPayload.class);
                case PLAN_MODE_CHANGE -> objectMapper.readValue(json, PlanModePayload.class);
                default -> null;
            };
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize payload for type " + type, e);
        }
    }

    private TurnEndReason deserializeTurnEndReason(String json) {
        try {
            if (json == null || json.isBlank()) return new TurnEndReason.Error("missing", "UNKNOWN");
            var node = objectMapper.readTree(json).get("reason");
            if (node == null) return new TurnEndReason.Error("missing reason", "UNKNOWN");
            String kind = node.has("type") ? node.get("type").asText() : "Completed";
            return switch (kind) {
                case "Completed" -> new TurnEndReason.Completed();
                case "Aborted" -> new TurnEndReason.Aborted(node.has("reason") ? node.get("reason").asText() : "");
                case "Blocked" -> new TurnEndReason.Blocked();
                case "Error" -> new TurnEndReason.Error(
                        node.has("message") ? node.get("message").asText() : "",
                        node.has("code") ? node.get("code").asText() : "UNKNOWN");
                case "MaxTokens" -> new TurnEndReason.MaxTokens();
                case "Interrupted" -> new TurnEndReason.Interrupted();
                default -> new TurnEndReason.Error("unknown: " + kind, "UNKNOWN");
            };
        } catch (Exception e) {
            return new TurnEndReason.Error("deserialize error: " + e.getMessage(), "UNKNOWN");
        }
    }

    private java.util.Map<String, Object> turnEndReasonToJson(TurnEndReason reason) {
        if (reason instanceof TurnEndReason.Completed) return java.util.Map.of("type", "Completed");
        if (reason instanceof TurnEndReason.Aborted r) return java.util.Map.of("type", "Aborted", "reason", r.reason());
        if (reason instanceof TurnEndReason.Blocked) return java.util.Map.of("type", "Blocked");
        if (reason instanceof TurnEndReason.Error r) return java.util.Map.of("type", "Error", "message", r.message(), "code", r.code());
        if (reason instanceof TurnEndReason.MaxTokens) return java.util.Map.of("type", "MaxTokens");
        if (reason instanceof TurnEndReason.Interrupted) return java.util.Map.of("type", "Interrupted");
        throw new IllegalStateException("Unknown TurnEndReason: " + reason.getClass());
    }

    private Path sessionDir(String sessionId) {
        // Sanitize sessionId to prevent path traversal
        String safe = sessionId.replaceAll("[^a-zA-Z0-9._-]", "_");
        return baseDir.resolve(safe);
    }
}
