package cn.xiaofuge.deepseek.harness.api.dto.terminal;

/**
 * 终端发送请求。
 * <p>
 * 执行流程：
 * 1. 前端传入命令和超时；
 * 2. API 层转成领域调用；
 * 3. 返回本次命令输出。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record TerminalSendRequest(String command, Long timeoutMs) {
}
