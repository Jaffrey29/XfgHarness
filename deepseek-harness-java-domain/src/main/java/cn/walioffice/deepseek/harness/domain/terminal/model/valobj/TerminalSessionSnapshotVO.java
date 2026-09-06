package cn.walioffice.deepseek.harness.domain.terminal.model.valobj;

import java.time.Instant;

/**
 * 终端会话快照。
 * <p>
 * 执行流程：
 * 1. 终端服务创建或刷新会话；
 * 2. 快照用于 API 返回；
 * 3. 前端只读展示状态与输出摘要。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record TerminalSessionSnapshotVO(
        String sessionId,
        String name,
        String cwd,
        TerminalSessionStatus status,
        Instant createdAt,
        Instant updatedAt,
        Integer exitCode,
        String lastMessage,
        int outputChars
) {
}
