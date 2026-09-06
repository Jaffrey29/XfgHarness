package cn.xiaofuge.deepseek.harness.api.response.terminal;

import cn.xiaofuge.deepseek.harness.domain.terminal.model.valobj.TerminalSendResultVO;
import java.time.Instant;

/**
 * 终端命令结果响应。
 * <p>
 * 执行流程：
 * 1. 读取命令执行结果；
 * 2. 映射为前端结构；
 * 3. 返回输出与终端状态。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record TerminalSendResponse(
        String sessionId,
        String command,
        String output,
        boolean completed,
        boolean truncated,
        String status,
        Integer exitCode,
        long durationMs,
        Instant completedAt
) {

    public static TerminalSendResponse from(TerminalSendResultVO result) {
        return new TerminalSendResponse(
                result.sessionId(),
                result.command(),
                result.output(),
                result.completed(),
                result.truncated(),
                result.status().name(),
                result.exitCode(),
                result.durationMs(),
                result.completedAt()
        );
    }
}
