package cn.walioffice.deepseek.harness.domain.agent.service;

import cn.walioffice.deepseek.harness.domain.llm.model.entity.Message;

/**
 * No-op implementation of {@link InboxNotifications}.
 * <p>
 * Suitable for testing and as a default when no live notification channel
 * is wired. Production code should supply a real implementation that
 * pushes to the UI / websocket / SSE layer.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public final class LoggingInboxNotifications implements InboxNotifications {

    public static final LoggingInboxNotifications INSTANCE = new LoggingInboxNotifications();

    @Override
    public void inserted(Message message) {
        // no-op
    }

    @Override
    public void discarded(Message message) {
        // no-op
    }

    @Override
    public void claimed(Message message, int turn) {
        // no-op
    }
}
