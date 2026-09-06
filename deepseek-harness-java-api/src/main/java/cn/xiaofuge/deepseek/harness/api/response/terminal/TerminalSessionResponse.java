package cn.xiaofuge.deepseek.harness.api.response.terminal;

import cn.xiaofuge.deepseek.harness.domain.terminal.model.valobj.TerminalSessionSnapshotVO;
import java.time.Instant;

/**
 * 终端会话响应。
 * <p>
 * 执行流程：
 * 1. 读取领域快照；
 * 2. 映射为前端可见字段；
 * 3. 返回会话状态与输出摘要。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record TerminalSessionResponse(
        String sessionId,
        String name,
        String cwd,
        String status,
        Instant createdAt,
        Instant updatedAt,
        Integer exitCode,
        String lastMessage,
        int outputChars
) {

    public static TerminalSessionResponse from(TerminalSessionSnapshotVO snapshot) {
        return new TerminalSessionResponse(
                snapshot.sessionId(),
                snapshot.name(),
                snapshot.cwd(),
                snapshot.status().name(),
                snapshot.createdAt(),
                snapshot.updatedAt(),
                snapshot.exitCode(),
                snapshot.lastMessage(),
                snapshot.outputChars()
        );
    }
}
