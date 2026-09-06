package cn.xiaofuge.deepseek.harness.domain.agent.service;

import cn.xiaofuge.deepseek.harness.domain.model.entity.Message;

/**
 * Live notifications committed by {@link Inbox} mutations.
 * <p>
 * Mirrors TS {@code InboxNotifications}. Each method is called after the
 * durable session event has been appended but before the live projection
 * is visible to the next reader, so synchronous observers see the pre-splice
 * state and can reconstruct removed messages from the normalized coordinates.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface InboxNotifications {

    /** Publish one inserted message. */
    void inserted(Message message);

    /** Publish one discarded message. */
    void discarded(Message message);

    /** Publish one claimed message inside its owning turn. */
    void claimed(Message message, int turn);
}
