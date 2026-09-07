package cn.xiaofuge.deepseek.harness.domain.session.event.service;

import cn.xiaofuge.deepseek.harness.domain.agent.model.entity.Phase;
import cn.xiaofuge.deepseek.harness.domain.agent.service.Inbox;
import cn.xiaofuge.deepseek.harness.domain.model.entity.Message;
import cn.xiaofuge.deepseek.harness.domain.session.ISessionEventLogService;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionEvent;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionHeader;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionLog;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.valobj.RequestHeaderPayload;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.valobj.RequestContextPayload;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.valobj.TodoWritePayload;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.valobj.TurnEndReason;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Reconstructs in-memory session state from a persisted event log.
 * <p>
 * Given a session id, this service:
 * <ol>
 *   <li>Loads the {@link SessionHeader} and all {@link SessionEvent}s from the
 *       {@link ISessionEventStore}</li>
 *   <li>Restores the in-memory {@link SessionLog} via
 *       {@link ISessionEventLogService#restore(String)}</li>
 *   <li>Rebuilds the {@link Inbox} by replaying inbox splice events</li>
 *   <li>Derives the current agent {@link Phase} from turn/step boundaries</li>
 *   <li>Extracts the latest todo snapshot, request header, and request context</li>
 * </ol>
 * The result is a {@link RestoredSession} snapshot that the agent loop can
 * resume from.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class SessionRebuilderService {

    private final ISessionEventLogService eventLogService;

    /**
     * 处理会话 Rebuilder Service。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param eventLogService 事件 Log Service参数。
     * @return 方法执行结果。
     */
    public SessionRebuilderService(ISessionEventLogService eventLogService) {
        this.eventLogService = eventLogService;
    }

    /**
     * Rebuild full session state from persisted events.
     *
     * @param sessionId session to restore
     * @return restored session snapshot
     */
    public RestoredSession rebuild(String sessionId) {
        // 1. Restore the SessionLog from the store
        eventLogService.restore(sessionId);

        SessionHeader header = eventLogService.getHeader(sessionId);
        List<SessionEvent> events = eventLogService.readAll(sessionId);

        // 2. Derive messages (surface projection)
        // SessionLog.deriveMessages() does the surface projection
        // We need to get the restored SessionLog — but it's internal to EventLogService.
        // Instead, we project from the raw events.
        List<Message> messages = projectMessages(events);

        // 3. Determine agent phase
        Phase phase = derivePhase(events);

        // 4. Extract latest todo snapshot
        Optional<TodoWritePayload> todos = extractLatestTodo(events);

        // 5. Extract latest request header
        Optional<RequestHeaderPayload> requestHeader = extractLatestRequestHeader(events);

        // 6. Extract latest request context
        Optional<RequestContextPayload> requestContext = extractLatestRequestContext(events);

        // 7. Count turns and steps
        int turnCount = countTurns(events);
        int stepCount = countSteps(events);

        // 8. Last turn end reason
        Optional<TurnEndReason> lastTurnEndReason = extractLastTurnEndReason(events);

        // 9. Last assistant message
        Optional<Message> lastAssistantMessage = extractLastAssistantMessage(events);

        return new RestoredSession(
                header,
                messages,
                phase,
                todos.orElse(null),
                requestHeader.orElse(null),
                requestContext.orElse(null),
                turnCount,
                stepCount,
                lastTurnEndReason.orElse(null),
                lastAssistantMessage.orElse(null),
                events.size()
        );
    }

    // ── Projection helpers ───────────────────────────────────────

    /**
     * 处理project Messages。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param events events参数。
     * @return 方法执行结果。
     */
    private List<Message> projectMessages(List<SessionEvent> events) {
        List<Message> messages = new ArrayList<>();
        for (SessionEvent event : events) {
            if (event instanceof SessionEvent.UserMessage um) {
                messages.add(um.data().message());
            } else if (event instanceof SessionEvent.AssistantMessage am) {
                messages.add(am.data().message());
            } else if (event instanceof SessionEvent.ToolResult tr) {
                messages.add(tr.data().message());
            }
        }
        return List.copyOf(messages);
    }

    /**
     * 处理derive Phase。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param events events参数。
     * @return 方法执行结果。
     */
    private Phase derivePhase(List<SessionEvent> events) {
        boolean inTurn = false;
        boolean inStep = false;
        for (SessionEvent event : events) {
            if (event instanceof SessionEvent.TurnStart) {
                inTurn = true;
                inStep = false;
            } else if (event instanceof SessionEvent.TurnEnd) {
                inTurn = false;
                inStep = false;
            } else if (event instanceof SessionEvent.StepStart) {
                inStep = true;
            } else if (event instanceof SessionEvent.StepEnd) {
                inStep = false;
            }
        }
        if (inStep) return new Phase.Running(0);
        if (inTurn) return new Phase.Maintenance(0);
        return new Phase.Idle(0);
    }

    /**
     * 提取 Latest 待办。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param events events参数。
     * @return 方法执行结果。
     */
    private Optional<TodoWritePayload> extractLatestTodo(List<SessionEvent> events) {
        TodoWritePayload latest = null;
        for (SessionEvent event : events) {
            if (event instanceof SessionEvent.TodoWrite tw) {
                latest = tw.data();
            }
        }
        return Optional.ofNullable(latest);
    }

    /**
     * 提取 Latest 请求 头信息。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param events events参数。
     * @return 方法执行结果。
     */
    private Optional<RequestHeaderPayload> extractLatestRequestHeader(List<SessionEvent> events) {
        RequestHeaderPayload latest = null;
        for (SessionEvent event : events) {
            if (event instanceof SessionEvent.RequestHeader rh) {
                latest = rh.data();
            }
        }
        return Optional.ofNullable(latest);
    }

    /**
     * 提取 Latest 请求 上下文。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param events events参数。
     * @return 方法执行结果。
     */
    private Optional<RequestContextPayload> extractLatestRequestContext(List<SessionEvent> events) {
        RequestContextPayload latest = null;
        for (SessionEvent event : events) {
            if (event instanceof SessionEvent.RequestContext rc) {
                latest = rc.data();
            }
        }
        return Optional.ofNullable(latest);
    }

    /**
     * 处理数量 Turns。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param events events参数。
     * @return 方法执行结果。
     */
    private int countTurns(List<SessionEvent> events) {
        int count = 0;
        for (SessionEvent event : events) {
            if (event instanceof SessionEvent.TurnStart) {
                count++;
            }
        }
        return count;
    }

    /**
     * 处理数量 Steps。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param events events参数。
     * @return 方法执行结果。
     */
    private int countSteps(List<SessionEvent> events) {
        int count = 0;
        for (SessionEvent event : events) {
            if (event instanceof SessionEvent.StepStart) {
                count++;
            }
        }
        return count;
    }

    /**
     * 提取 Last Turn End Reason。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param events events参数。
     * @return 方法执行结果。
     */
    private Optional<TurnEndReason> extractLastTurnEndReason(List<SessionEvent> events) {
        TurnEndReason reason = null;
        for (SessionEvent event : events) {
            if (event instanceof SessionEvent.TurnEnd te) {
                reason = te.reason();
            }
        }
        return Optional.ofNullable(reason);
    }

    /**
     * 提取 Last Assistant 消息。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param events events参数。
     * @return 方法执行结果。
     */
    private Optional<Message> extractLastAssistantMessage(List<SessionEvent> events) {
        Message last = null;
        for (SessionEvent event : events) {
            if (event instanceof SessionEvent.AssistantMessage am) {
                last = am.data().message();
            }
        }
        return Optional.ofNullable(last);
    }

    // ── Result record ────────────────────────────────────────────

    /**
     * Immutable snapshot of restored session state.
     */
    public record RestoredSession(
            SessionHeader header,
            List<Message> messages,
            Phase phase,
            TodoWritePayload todos,
            RequestHeaderPayload requestHeader,
            RequestContextPayload requestContext,
            int turnCount,
            int stepCount,
            TurnEndReason lastTurnEndReason,
            Message lastAssistantMessage,
            int totalEvents
    ) {}
}
