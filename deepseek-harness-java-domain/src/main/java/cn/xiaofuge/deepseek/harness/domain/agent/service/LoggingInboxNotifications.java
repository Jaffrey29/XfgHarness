package cn.xiaofuge.deepseek.harness.domain.agent.service;

import cn.xiaofuge.deepseek.harness.domain.model.entity.Message;

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

    /**
     * claimed。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param message 消息内容或对象。
     * @param turn 当前回合。
     */

    /**
     * claimed。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param message 消息内容或对象。
     * @param turn 当前回合。
     */

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
