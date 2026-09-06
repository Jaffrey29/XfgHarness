package cn.xiaofuge.deepseek.harness.domain.terminal.model.valobj;

import java.time.Instant;

/**
 * 发送命令后的结果。
 * <p>
 * 执行流程：
 * 1. 写入命令到会话 stdin；
 * 2. 收集新输出片段；
 * 3. 返回执行状态与耗时信息。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record TerminalSendResultVO(
        String sessionId,
        String command,
        String output,
        boolean completed,
        boolean truncated,
        TerminalSessionStatus status,
        Integer exitCode,
        long durationMs,
        Instant completedAt
) {
}
