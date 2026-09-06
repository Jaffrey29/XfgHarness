package cn.walioffice.deepseek.harness.domain.agent.service;

import cn.walioffice.deepseek.harness.domain.agent.model.valobj.InboxSplice;
import cn.walioffice.deepseek.harness.domain.agent.model.valobj.InboxTarget;
import cn.walioffice.deepseek.harness.domain.llm.model.entity.Message;
import cn.walioffice.deepseek.harness.domain.session.event.model.entity.SessionEvent;
import cn.walioffice.deepseek.harness.domain.session.event.model.entity.SessionLog;
import cn.walioffice.deepseek.harness.domain.session.event.model.valobj.InboxSplicedPayload;
import cn.walioffice.deepseek.harness.domain.session.event.service.SessionEventFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Incremental projection of durable agent inbox events.
 * <p>
 * Mirrors TS {@code Inbox}. An {@code Inbox} owns two ordered pending-message
 * lists ({@code next-turn} and {@code next-step}) and durably records every
 * mutation as an {@code agent/inbox/spliced} session event.
 * <p>
 * <strong>Replay semantics:</strong> On construction, the inbox replays all
 * {@code agent/inbox/spliced} events from the session log (skipping the seed
 * portion). After replay, the inbox is live — new mutations append events
 * to the session log and update the in-memory state synchronously.
 * <p>
 * <strong>Durability ordering:</strong> The durable event is appended
 * <em>before</em> the live projection mutates, so synchronous session-event
 * observers see the pre-splice lists and can reconstruct removed messages
 * from the normalized coordinates.
 * <p>
 * <strong>Identity uniqueness:</strong> No two pending messages in either
 * list may share the same id. Validation runs against the combined
 * post-splice state (both lists) to prevent duplicates.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public final class Inbox {

    private final SessionLog session;
    private final InboxNotifications notifications;
    private final List<Message> nextTurn = new ArrayList<>();
    private final List<Message> nextStep = new ArrayList<>();

    /**
     * Construct an inbox by replaying durable inbox events from the session.
     *
     * @param session       the session log (must be past the seed)
     * @param notifications live notification sink
     */
    public Inbox(SessionLog session, InboxNotifications notifications) {
        this.session = session;
        this.notifications = notifications;
        replay();
    }

    private void replay() {
        int seedLength = session.header() != null
                ? session.header().seedLength() : 0;
        List<SessionEvent> events = session.events();
        for (int i = seedLength; i < events.size(); i++) {
            SessionEvent event = events.get(i);
            if (!(event instanceof SessionEvent.AgentInboxSpliced spliced)) continue;
            try {
                apply(spliced.data());
            } catch (RuntimeException e) {
                throw new IllegalStateException(
                        "invalid persisted inbox splice at session seq " + event.seq(), e);
            }
        }
    }

    /** Prompts awaiting individual turns (read-only view). */
    public List<Message> nextTurn() {
        return List.copyOf(nextTurn);
    }

    /** Input awaiting the next step boundary (read-only view). */
    public List<Message> nextStep() {
        return List.copyOf(nextStep);
    }

    /** Whether either pending-message list contains work. */
    public boolean hasPending() {
        return !nextTurn.isEmpty() || !nextStep.isEmpty();
    }

    /** Durably cancel all pending input, clearing next-step before next-turn. */
    public void clear() {
        splice(InboxTarget.NEXT_STEP, 0, nextStep.size(), List.of());
        splice(InboxTarget.NEXT_TURN, 0, nextTurn.size(), List.of());
    }

    /**
     * Remove and return the complete batch proposed for one step, publishing
     * each claimed message. The durable splices are pure deletions.
     *
     * @param target whether this boundary also consumes one queued turn
     * @param turn   turn that will own the claimed batch
     * @return next-step input followed by the queued turn, when requested
     */
    public List<Message> claim(InboxTarget target, int turn) {
        List<Message> claimed = new ArrayList<>(mutate(InboxTarget.NEXT_STEP, 0, nextStep.size(), List.of(), false));
        if (target == InboxTarget.NEXT_TURN) {
            claimed.addAll(mutate(InboxTarget.NEXT_TURN, 0, 1, List.of(), false));
        }
        for (Message message : claimed) {
            notifications.claimed(message, turn);
        }
        return claimed;
    }

    /**
     * Append one message to a pending list and durably record the insertion.
     *
     * @param target  pending list to extend
     * @param message message to append
     * @throws IllegalStateException if the message identity is already pending
     */
    public void append(InboxTarget target, Message message) {
        splice(target, listFor(target).size(), 0, List.of(message));
    }

    /**
     * Prepend one message to a pending list and durably record the insertion.
     *
     * @param target  pending list to extend
     * @param message message to prepend
     * @throws IllegalStateException if the message identity is already pending
     */
    public void prepend(InboxTarget target, Message message) {
        splice(target, 0, 0, List.of(message));
    }

    /**
     * Replace one pending message in place, possibly changing its identity.
     * A successful replacement publishes the old message as discarded and
     * the new message as inserted.
     *
     * @param messageId  identity of the pending message to replace
     * @param newMessage replacement message
     * @return whether the message was still pending
     * @throws IllegalStateException if the replacement duplicates another pending message identity
     */
    public boolean replace(String messageId, Message newMessage) {
        Optional<Location> location = locate(messageId);
        if (location.isEmpty()) return false;
        Location loc = location.get();
        splice(loc.target(), loc.index(), 1, List.of(newMessage));
        return true;
    }

    /**
     * Remove one pending message and durably record its cancellation.
     *
     * @param messageId identity of the pending message to remove
     * @return whether the message was still pending
     */
    public boolean remove(String messageId) {
        Optional<Location> location = locate(messageId);
        if (location.isEmpty()) return false;
        Location loc = location.get();
        splice(loc.target(), loc.index(), 1, List.of());
        return true;
    }

    /**
     * Apply standard splice semantics and durably record the normalized result.
     * <p>
     * The durable event commits before the live projection mutates.
     *
     * @param target      pending list to mutate
     * @param start       splice position
     * @param deleteCount maximum number of messages to remove
     * @param inserted    messages to insert at the resolved position
     * @return messages removed by the splice
     */
    public List<Message> splice(InboxTarget target, int start, int deleteCount, List<Message> inserted) {
        return mutate(target, start, deleteCount, inserted, true);
    }

    // ── Internal ──────────────────────────────────────────────────

    private List<Message> listFor(InboxTarget target) {
        return target == InboxTarget.NEXT_TURN ? nextTurn : nextStep;
    }

    private Optional<Location> locate(String messageId) {
        for (InboxTarget target : InboxTarget.values()) {
            List<Message> list = listFor(target);
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).id().equals(messageId)) {
                    return Optional.of(new Location(target, i));
                }
            }
        }
        return Optional.empty();
    }

    private List<Message> mutate(
            InboxTarget target,
            int start,
            int deleteCount,
            List<Message> inserted,
            boolean discardRemoved
    ) {
        List<Message> inbox = listFor(target);

        // Normalize start (truncate → clamp, support negative as offset from end)
        int truncatedStart = (int) start; // already int in Java
        int offset = truncatedStart;
        int actualStart = offset < 0
                ? Math.max(inbox.size() + offset, 0)
                : Math.min(offset, inbox.size());

        // Normalize deleteCount
        int truncatedDeleteCount = deleteCount;
        int actualDeleteCount = Math.min(
                Math.max(truncatedDeleteCount, 0),
                inbox.size() - actualStart
        );

        if (actualDeleteCount == 0 && inserted.isEmpty()) {
            return List.of();
        }

        String outcome = (discardRemoved && actualDeleteCount > 0) ? "canceled" : null;
        var splice = new InboxSplice(target, actualStart, actualDeleteCount, inserted, outcome);
        validate(splice);

        // Append durable event BEFORE mutating live state
        var event = SessionEventFactory.inboxSpliced(
                target.wireName(), actualStart, actualDeleteCount, inserted, outcome);
        session.append(event);

        // Mutate live state
        List<Message> removed = new ArrayList<>();
        for (int i = 0; i < actualDeleteCount; i++) {
            removed.add(inbox.remove(actualStart));
        }
        for (int i = inserted.size() - 1; i >= 0; i--) {
            inbox.add(actualStart, inserted.get(i));
        }

        // Publish notifications
        if (discardRemoved) {
            for (Message message : removed) {
                notifications.discarded(message);
            }
        }
        for (Message message : inserted) {
            notifications.inserted(message);
        }

        return removed;
    }

    /** Apply one normalized durable splice to the projection (during replay). */
    private List<Message> apply(InboxSplicedPayload data) {
        InboxTarget target = InboxTarget.fromWireName(data.target());
        int removedCount = data.removedCount();
        List<Message> inserted = data.inserted();

        // Validate
        var splice = new InboxSplice(target, data.start(), removedCount, inserted, data.outcome());
        validate(splice);

        List<Message> inbox = listFor(target);
        List<Message> removed = new ArrayList<>();
        for (int i = 0; i < removedCount; i++) {
            removed.add(inbox.remove(data.start()));
        }
        for (int i = inserted.size() - 1; i >= 0; i--) {
            inbox.add(data.start(), inserted.get(i));
        }
        return removed;
    }

    /**
     * Validate one normalized splice against the current projection.
     *
     * @throws IllegalStateException if the splice is out of bounds or creates duplicate ids
     */
    private void validate(InboxSplice splice) {
        List<Message> inbox = listFor(splice.target());
        int removedCount = splice.removedCount();

        if (splice.start() < 0 || splice.start() > inbox.size()
                || removedCount < 0
                || splice.start() + removedCount > inbox.size()) {
            throw new IllegalStateException("invalid inbox splice: target=" + splice.target()
                    + " start=" + splice.start() + " removedCount=" + removedCount
                    + " listSize=" + inbox.size());
        }

        // Build candidate post-splice list
        List<Message> candidate = new ArrayList<>(inbox);
        for (int i = 0; i < removedCount; i++) {
            candidate.remove(splice.start());
        }
        for (int i = splice.inserted().size() - 1; i >= 0; i--) {
            candidate.add(splice.start(), splice.inserted().get(i));
        }

        // Check for duplicate ids across both lists
        java.util.Set<String> ids = new java.util.HashSet<>();
        List<Message> other = splice.target() == InboxTarget.NEXT_TURN ? nextStep : nextTurn;
        for (Message message : candidate) {
            if (!ids.add(message.id())) {
                throw new IllegalStateException(
                        "message \"" + message.id() + "\" is already pending");
            }
        }
        for (Message message : other) {
            if (!ids.add(message.id())) {
                throw new IllegalStateException(
                        "message \"" + message.id() + "\" is already pending");
            }
        }
    }

    private record Location(InboxTarget target, int index) {}
}
