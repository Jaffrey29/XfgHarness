package cn.walioffice.deepseek.harness.domain.agent.model.valobj;

import cn.walioffice.deepseek.harness.domain.llm.model.entity.Message;
import java.util.List;

/**
 * One normalized inbox splice mutation.
 * <p>
 * Mirrors the TS {@code SessionEventMap['agent/inbox/spliced']} data shape.
 * This is the durable, validated form of an inbox mutation that gets
 * appended to the session event log.
 *
 * @param target       which pending list to mutate
 * @param start        splice position (0-based, normalized to [0, list.length])
 * @param removedCount number of messages removed (0 if none)
 * @param inserted     messages inserted at the splice position
 * @param outcome      "canceled" if removed messages were discarded, null otherwise
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record InboxSplice(
        InboxTarget target,
        int start,
        int removedCount,
        List<Message> inserted,
        String outcome
) {
    public InboxSplice {
        inserted = inserted == null ? List.of() : List.copyOf(inserted);
    }
}
