package cn.walioffice.deepseek.harness.domain.session.event.model.valobj;

import cn.walioffice.deepseek.harness.domain.llm.model.entity.Message;
import java.util.List;

/**
 * Payload for {@code agent/inbox/spliced} session events.
 * <p>
 * Mirrors TS {@code SessionEventMap['agent/inbox/spliced']}.
 * The {@code inserted} list contains full {@link Message} objects (not just ids),
 * matching the TS contract where UserMessage[] is persisted.
 *
 * @param target       "next-turn" or "next-step"
 * @param start        splice position (0-based, normalized)
 * @param removedCount number of messages removed (0 if none)
 * @param inserted     messages inserted at the splice position
 * @param outcome      "canceled" if removed messages were discarded, null otherwise
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record InboxSplicedPayload(
        String target,
        int start,
        int removedCount,
        List<Message> inserted,
        String outcome
) {
    public InboxSplicedPayload {
        inserted = inserted == null ? List.of() : List.copyOf(inserted);
    }
}
