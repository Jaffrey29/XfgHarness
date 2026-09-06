package cn.walioffice.deepseek.harness.api.service.command;

import cn.walioffice.deepseek.harness.api.dto.terminal.TerminalOpenRequest;
import cn.walioffice.deepseek.harness.api.dto.terminal.TerminalSendRequest;
import cn.walioffice.deepseek.harness.api.response.terminal.TerminalSendResponse;
import cn.walioffice.deepseek.harness.api.response.terminal.TerminalSessionResponse;
import cn.walioffice.deepseek.harness.domain.terminal.ITerminalCommandService;
import org.springframework.stereotype.Service;

/**
 * 终端命令适配器。
 * <p>
 * 执行流程：
 * 1. 接收打开、发送、关闭请求；
 * 2. 转交终端领域服务；
 * 3. 返回最新会话状态。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class TerminalCommandApi {

    private final ITerminalCommandService terminalCommandService;

    public TerminalCommandApi(ITerminalCommandService terminalCommandService) {
        this.terminalCommandService = terminalCommandService;
    }

    public TerminalSessionResponse open(TerminalOpenRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("terminal open request must not be null");
        }
        return TerminalSessionResponse.from(terminalCommandService.open(request.name(), request.cwd()));
    }

    public TerminalSendResponse send(String sessionId, TerminalSendRequest request) {
        validateSessionId(sessionId);
        if (request == null) {
            throw new IllegalArgumentException("terminal send request must not be null");
        }
        long timeoutMs = request.timeoutMs() == null ? 120_000L : request.timeoutMs();
        return TerminalSendResponse.from(terminalCommandService.send(sessionId, request.command(), timeoutMs));
    }

    public TerminalSessionResponse close(String sessionId) {
        validateSessionId(sessionId);
        return TerminalSessionResponse.from(terminalCommandService.close(sessionId));
    }

    private static void validateSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
    }
}
