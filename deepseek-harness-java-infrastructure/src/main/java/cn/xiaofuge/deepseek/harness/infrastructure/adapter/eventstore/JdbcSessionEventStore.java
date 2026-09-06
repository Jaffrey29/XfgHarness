package cn.xiaofuge.deepseek.harness.infrastructure.adapter.eventstore;

import cn.xiaofuge.deepseek.harness.domain.session.event.adapter.repository.ISessionEventStore;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionEvent;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionHeader;
import cn.xiaofuge.deepseek.harness.infrastructure.dao.ISessionEventRowDao;
import cn.xiaofuge.deepseek.harness.infrastructure.dao.po.SessionEventRowPO;
import cn.xiaofuge.deepseek.harness.infrastructure.dao.po.SessionHeaderRowPO;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.valobj.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/**
 * JDBC-backed implementation of {@link ISessionEventStore}.
 * <p>
 * Persists typed session events and headers via MyBatis DAO.
 * Payloads are serialized as JSON.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Repository
public class JdbcSessionEventStore implements ISessionEventStore {

    private final ISessionEventRowDao dao;
    private final ObjectMapper objectMapper;

    public JdbcSessionEventStore(ISessionEventRowDao dao) {
        this.dao = dao;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules(); // for Instant support
    }

    @Override
    public void saveHeader(SessionHeader header) {
        dao.upsertHeader(
                header.sessionId(),
                header.version(),
                header.createdAt(),
                header.cwd(),
                header.parentSession(),
                header.seedLength(),
                header.origin(),
                header.delegationDepth(),
                header.agentPreset()
        );
    }

    @Override
    public Optional<SessionHeader> getHeader(String sessionId) {
        SessionHeaderRowPO row = dao.getHeader(sessionId);
        if (row == null) {
            return Optional.empty();
        }
        return Optional.of(new SessionHeader(
                row.version(),
                row.sessionId(),
                row.createdAt(),
                row.cwd(),
                row.parentSession(),
                row.seedLength(),
                row.origin(),
                row.delegationDepth(),
                row.agentPreset()
        ));
    }

    @Override
    public void append(String sessionId, SessionEvent event) {
        dao.insert(toRow(sessionId, event));
    }

    @Override
    public void appendBatch(String sessionId, List<SessionEvent> events) {
        for (SessionEvent event : events) {
            dao.insert(toRow(sessionId, event));
        }
    }

    @Override
    public List<SessionEvent> readAll(String sessionId) {
        return dao.queryBySessionId(sessionId).stream()
                .map(this::toEvent)
                .toList();
    }

    @Override
    public List<SessionEvent> readAfter(String sessionId, long afterSeq) {
        return dao.queryAfter(sessionId, afterSeq).stream()
                .map(this::toEvent)
                .toList();
    }

    @Override
    public List<SessionEvent> readRange(String sessionId, long fromSeq, long toSeq) {
        return dao.queryRange(sessionId, fromSeq, toSeq).stream()
                .map(this::toEvent)
                .toList();
    }

    @Override
    public long getLastSeq(String sessionId) {
        Long result = dao.getMaxSeq(sessionId);
        return result == null ? -1 : result;
    }

    @Override
    public long count(String sessionId) {
        return dao.count(sessionId);
    }

    @Override
    public List<SessionHeader> listHeaders(int limit) {
        return dao.listHeaders(limit).stream()
                .map(row -> new SessionHeader(
                        row.version(),
                        row.sessionId(),
                        row.createdAt(),
                        row.cwd(),
                        row.parentSession(),
                        row.seedLength(),
                        row.origin(),
                        row.delegationDepth(),
                        row.agentPreset()
                ))
                .toList();
    }

    // ── Serialization ────────────────────────────────────────────

    private SessionEventRowPO toRow(String sessionId, SessionEvent event) {
        String payloadJson = serializePayload(event);
        String surfaceOp = serializeSurfaceOp(event);
        String sourceSeqs = serializeSourceSeqs(event);
        return new SessionEventRowPO(
                sessionId,
                event.seq(),
                event.type().wireName(),
                event.time(),
                payloadJson,
                surfaceOp,
                sourceSeqs
        );
    }

    private SessionEvent toEvent(SessionEventRowPO row) {
        SessionEventType type = SessionEventType.fromWireName(row.eventType());
        Object payload = deserializePayload(type, row.payloadJson());
        SurfaceIntent surfaceIntent = deserializeSurfaceIntent(row.surfaceOp(), row.sourceEventSeqs());

        return switch (type) {
            case TURN_START -> new SessionEvent.TurnStart(row.seq(), row.eventTime());
            case TURN_END -> {
                TurnEndReason reason = deserializeTurnEndReason(row.payloadJson());
                yield new SessionEvent.TurnEnd(row.seq(), row.eventTime(), reason);
            }
            case STEP_START -> {
                StepStartPayload p = (StepStartPayload) payload;
                yield new SessionEvent.StepStart(row.seq(), row.eventTime(), p.turn(), p.step());
            }
            case STEP_END -> {
                StepEndPayload p = (StepEndPayload) payload;
                yield new SessionEvent.StepEnd(row.seq(), row.eventTime(), p.turn(), p.step());
            }
            case USER_MESSAGE -> new SessionEvent.UserMessage(row.seq(), row.eventTime(), (UserMessagePayload) payload, surfaceIntent);
            case ASSISTANT_CHUNK -> new SessionEvent.AssistantChunk(row.seq(), row.eventTime(), (AssistantChunkPayload) payload);
            case ASSISTANT_MESSAGE -> new SessionEvent.AssistantMessage(row.seq(), row.eventTime(), (AssistantMessagePayload) payload, surfaceIntent);
            case TOOL_CALL -> new SessionEvent.ToolCall(row.seq(), row.eventTime(), (ToolCallPayload) payload);
            case TOOL_RESULT -> new SessionEvent.ToolResult(row.seq(), row.eventTime(), (ToolResultPayload) payload, surfaceIntent);
            case TODO_WRITE -> new SessionEvent.TodoWrite(row.seq(), row.eventTime(), (TodoWritePayload) payload);
            case REQUEST_HEADER -> new SessionEvent.RequestHeader(row.seq(), row.eventTime(), (RequestHeaderPayload) payload);
            case REQUEST_CONTEXT -> new SessionEvent.RequestContext(row.seq(), row.eventTime(), (RequestContextPayload) payload);
            case SESSION_END_SEED -> new SessionEvent.SessionEndSeed(row.seq(), row.eventTime());
            case AGENT_INBOX_SPLICED -> new SessionEvent.AgentInboxSpliced(row.seq(), row.eventTime(), (InboxSplicedPayload) payload);
            case PLAN_MODE_CHANGE -> new SessionEvent.PlanModeChange(row.seq(), row.eventTime(), (PlanModePayload) payload);
            case TASK_SUBMITTED, TASK_AUTO_APPROVED, TASK_APPROVAL_REQUIRED,
                 TASK_APPROVED, TASK_EXECUTION_STARTED,
                 TASK_EXECUTION_COMPLETED, TASK_EXECUTION_FAILED ->
                new SessionEvent.Generic(row.seq(), row.eventTime(), type, row.payloadJson());
        };
    }

    private String serializePayload(SessionEvent event) {
        try {
            if (event instanceof SessionEvent.TurnStart) {
            return objectMapper.writeValueAsString(java.util.Map.of());
        } else if (event instanceof SessionEvent.TurnEnd e) {
            return objectMapper.writeValueAsString(java.util.Map.of("reason", turnEndReasonToJson(e.reason())));
        } else if (event instanceof SessionEvent.StepStart e) {
            return objectMapper.writeValueAsString(java.util.Map.of("turn", e.turn(), "step", e.step()));
        } else if (event instanceof SessionEvent.StepEnd e) {
            return objectMapper.writeValueAsString(java.util.Map.of("turn", e.turn(), "step", e.step()));
        } else if (event instanceof SessionEvent.UserMessage e) {
            return objectMapper.writeValueAsString(e.data());
        } else if (event instanceof SessionEvent.AssistantChunk e) {
            return objectMapper.writeValueAsString(e.data());
        } else if (event instanceof SessionEvent.AssistantMessage e) {
            return objectMapper.writeValueAsString(e.data());
        } else if (event instanceof SessionEvent.ToolCall e) {
            return objectMapper.writeValueAsString(e.data());
        } else if (event instanceof SessionEvent.ToolResult e) {
            return objectMapper.writeValueAsString(e.data());
        } else if (event instanceof SessionEvent.TodoWrite e) {
            return objectMapper.writeValueAsString(e.data());
        } else if (event instanceof SessionEvent.RequestHeader e) {
            return objectMapper.writeValueAsString(e.data());
        } else if (event instanceof SessionEvent.RequestContext e) {
            return objectMapper.writeValueAsString(e.data());
        } else if (event instanceof SessionEvent.SessionEndSeed) {
            return objectMapper.writeValueAsString(java.util.Map.of());
        } else if (event instanceof SessionEvent.AgentInboxSpliced e) {
            return objectMapper.writeValueAsString(e.data());
        } else if (event instanceof SessionEvent.PlanModeChange e) {
            return objectMapper.writeValueAsString(e.data());
        } else if (event instanceof SessionEvent.Generic e) {
            return e.payload() == null ? "" : e.payload();
        } else {
            throw new IllegalStateException("Unknown event variant: " + event.getClass());
        }
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Failed to serialize event payload", ex);
        }
    }

    private String serializeSurfaceOp(SessionEvent event) {
        SurfaceIntent intent = event.surfaceIntent();
        if (intent == null || intent.surfaceOp() == null) {
            return null;
        }
        if (intent.surfaceOp() instanceof SurfaceOp.Append) {
            return "append";
        } else if (intent.surfaceOp() instanceof SurfaceOp.Replace r) {
            return "replace:" + r.start() + ":" + r.end();
        } else {
            return null;
        }
    }

    private String serializeSourceSeqs(SessionEvent event) {
        SurfaceIntent intent = event.surfaceIntent();
        if (intent == null || intent.sourceEventSeqs() == null || intent.sourceEventSeqs().isEmpty()) {
            return null;
        }
        return intent.sourceEventSeqs().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private SurfaceIntent deserializeSurfaceIntent(String surfaceOpStr, String sourceSeqsStr) {
        if (surfaceOpStr == null || surfaceOpStr.isBlank()) {
            return null;
        }
        SurfaceOp op;
        if ("append".equals(surfaceOpStr)) {
            op = new SurfaceOp.Append();
        } else if (surfaceOpStr.startsWith("replace:")) {
            String[] parts = surfaceOpStr.split(":");
            op = new SurfaceOp.Replace(Long.parseLong(parts[1]), Long.parseLong(parts[2]));
        } else {
            return null;
        }
        List<Long> seqs = null;
        if (sourceSeqsStr != null && !sourceSeqsStr.isBlank()) {
            seqs = java.util.Arrays.stream(sourceSeqsStr.split(","))
                    .map(Long::parseLong)
                    .toList();
        }
        return new SurfaceIntent(op, seqs);
    }

    private Object deserializePayload(SessionEventType type, String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
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
                default -> null;
            };
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize payload for type " + type, e);
        }
    }

    private TurnEndReason deserializeTurnEndReason(String json) {
        try {
            var node = objectMapper.readTree(json);
            var reasonNode = node.get("reason");
            if (reasonNode == null) {
                return new TurnEndReason.Error("unknown", "UNKNOWN");
            }
            var kindNode = reasonNode.get("type");
            String kind = kindNode != null ? kindNode.asText() : "Completed";
            return switch (kind) {
                case "Completed" -> new TurnEndReason.Completed();
                case "Aborted" -> new TurnEndReason.Aborted(reasonNode.has("reason") ? reasonNode.get("reason").asText() : "");
                case "Blocked" -> new TurnEndReason.Blocked();
                case "Error" -> new TurnEndReason.Error(
                        reasonNode.has("message") ? reasonNode.get("message").asText() : "",
                        reasonNode.has("code") ? reasonNode.get("code").asText() : "UNKNOWN");
                case "MaxTokens" -> new TurnEndReason.MaxTokens();
                case "Interrupted" -> new TurnEndReason.Interrupted();
                default -> new TurnEndReason.Error("unknown reason: " + kind, "UNKNOWN");
            };
        } catch (Exception e) {
            return new TurnEndReason.Error("failed to deserialize: " + e.getMessage(), "UNKNOWN");
        }
    }

    private java.util.Map<String, Object> turnEndReasonToJson(TurnEndReason reason) {
        if (reason instanceof TurnEndReason.Completed) {
            return java.util.Map.of("type", "Completed");
        } else if (reason instanceof TurnEndReason.Aborted r) {
            return java.util.Map.of("type", "Aborted", "reason", r.reason());
        } else if (reason instanceof TurnEndReason.Blocked) {
            return java.util.Map.of("type", "Blocked");
        } else if (reason instanceof TurnEndReason.Error r) {
            return java.util.Map.of("type", "Error", "message", r.message(), "code", r.code());
        } else if (reason instanceof TurnEndReason.MaxTokens) {
            return java.util.Map.of("type", "MaxTokens");
        } else if (reason instanceof TurnEndReason.Interrupted) {
            return java.util.Map.of("type", "Interrupted");
        } else {
            throw new IllegalStateException("Unknown TurnEndReason: " + reason.getClass());
        }
    }
}
