package cn.xiaofuge.deepseek.harness.domain.terminal.model.valobj;

/**
 * 终端输出读取结果。
 * <p>
 * 执行流程：
 * 1. 读取当前会话缓冲区；
 * 2. 只返回最近输出片段；
 * 3. 保留是否截断的信息。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record TerminalReadResultVO(
        String sessionId,
        String content,
        boolean truncated,
        TerminalSessionStatus status,
        Integer exitCode
) {
}
