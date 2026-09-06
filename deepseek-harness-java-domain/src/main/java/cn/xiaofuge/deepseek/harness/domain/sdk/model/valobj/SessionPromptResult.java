package cn.xiaofuge.deepseek.harness.domain.sdk.model.valobj;

/**
 * Durable enqueue receipt for one prompt.
 *
 * @param messageId identity of the queued user message
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record SessionPromptResult(String messageId) {}
