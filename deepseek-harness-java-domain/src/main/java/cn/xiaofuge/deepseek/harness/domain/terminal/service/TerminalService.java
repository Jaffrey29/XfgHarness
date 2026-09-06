package cn.xiaofuge.deepseek.harness.domain.terminal.service;

import cn.xiaofuge.deepseek.harness.domain.terminal.ITerminalCommandService;
import cn.xiaofuge.deepseek.harness.domain.terminal.ITerminalQueryService;
import cn.xiaofuge.deepseek.harness.domain.terminal.adapter.port.ITerminalPort;
import cn.xiaofuge.deepseek.harness.domain.terminal.model.valobj.TerminalReadResultVO;
import cn.xiaofuge.deepseek.harness.domain.terminal.model.valobj.TerminalSendResultVO;
import cn.xiaofuge.deepseek.harness.domain.terminal.model.valobj.TerminalSessionSnapshotVO;
import java.util.List;

/**
 * 终端领域服务。
 * <p>
 * 执行流程：
 * 1. 校验会话参数；
 * 2. 调用终端端口执行本地进程操作；
 * 3. 返回会话快照和输出结果。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class TerminalService implements ITerminalCommandService, ITerminalQueryService {

    private final ITerminalPort terminalPort;

    public TerminalService(ITerminalPort terminalPort) {
        this.terminalPort = terminalPort;
    }

    @Override
    public TerminalSessionSnapshotVO open(String name, String cwd) {
        return terminalPort.open(name, cwd);
    }

    @Override
    public TerminalSendResultVO send(String sessionId, String command, long timeoutMs) {
        validateSessionId(sessionId);
        validateCommand(command);
        return terminalPort.send(sessionId, command, timeoutMs);
    }

    @Override
    public TerminalSessionSnapshotVO close(String sessionId) {
        validateSessionId(sessionId);
        return terminalPort.close(sessionId);
    }

    @Override
    public TerminalReadResultVO read(String sessionId, int maxChars) {
        validateSessionId(sessionId);
        return terminalPort.read(sessionId, maxChars);
    }

    @Override
    public List<TerminalSessionSnapshotVO> list() {
        return terminalPort.list();
    }

    private static void validateSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
    }

    private static void validateCommand(String command) {
        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException("command must not be blank");
        }
    }
}
