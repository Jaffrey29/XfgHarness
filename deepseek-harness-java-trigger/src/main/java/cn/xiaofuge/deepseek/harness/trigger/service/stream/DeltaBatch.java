package cn.xiaofuge.deepseek.harness.trigger.service.stream;

import java.util.function.Consumer;

/**
 * 流式增量聚合缓冲：把 LLM 逐 token 回调的高频小 delta 攒成较大分片推送。
 * <p>
 * 背景：逐 token 推 SSE 会让前端每帧重渲染整条消息，长对话时表现为明显闪烁。
 * 聚合到 60ms 窗口后，前端渲染频率从每秒数百次降到约 16 次，肉眼流畅且延迟无感。
 * <p>
 * 触发时机：accept 时距上次推送超过 {@link #FLUSH_INTERVAL_MS}，或流收尾
 * （finish/done/error/tool_call 前）显式 {@link #flush()}。无后台定时线程。
 * 发送在锁内完成，保证多线程下分片顺序与接收顺序一致。
 */
final class DeltaBatch {

    private static final long FLUSH_INTERVAL_MS = 60L;

    private final Consumer<String> sender;
    private final StringBuilder buffer = new StringBuilder();
    private long lastFlushAt = System.currentTimeMillis();

    DeltaBatch(Consumer<String> sender) {
        this.sender = sender;
    }

    synchronized void accept(String delta) {
        if (delta == null || delta.isEmpty()) {
            return;
        }
        buffer.append(delta);
        long now = System.currentTimeMillis();
        if (now - lastFlushAt >= FLUSH_INTERVAL_MS) {
            flush(now);
        }
    }

    synchronized void flush() {
        flush(System.currentTimeMillis());
    }

    private void flush(long now) {
        if (buffer.length() == 0) {
            lastFlushAt = now;
            return;
        }
        String content = buffer.toString();
        buffer.setLength(0);
        lastFlushAt = now;
        sender.accept(content);
    }
}
