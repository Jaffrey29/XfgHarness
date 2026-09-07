package cn.xiaofuge.deepseek.harness.domain.terminal.service;

import cn.xiaofuge.deepseek.harness.domain.terminal.ITerminalCommandService;
import cn.xiaofuge.deepseek.harness.domain.terminal.ITerminalQueryService;
import cn.xiaofuge.deepseek.harness.domain.terminal.adapter.port.ITerminalPort;
import cn.xiaofuge.deepseek.harness.domain.terminal.model.valobj.TerminalReadResultVO;
import cn.xiaofuge.deepseek.harness.domain.terminal.model.valobj.TerminalSendResultVO;
import cn.xiaofuge.deepseek.harness.domain.terminal.model.valobj.TerminalSessionSnapshotVO;
import java.util.List;
import org.springframework.stereotype.Service;

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
@Service
public class TerminalService implements ITerminalCommandService, ITerminalQueryService {

    private final ITerminalPort terminalPort;

    /**
     * 处理Terminal Service相关逻辑。
     */
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
    /**
     * 列出。
     */
    public TerminalReadResultVO read(String sessionId, int maxChars) {
        validateSessionId(sessionId);
        return terminalPort.read(sessionId, maxChars);
    }

    @Override
    public List<TerminalSessionSnapshotVO> list() {
        return terminalPort.list();
    }

    /**
     * 校验输入与业务规则；不通过时返回失败结果或抛出异常。
     */
    private static void validateSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
    }

    /**
     * 校验输入与业务规则；不通过时返回失败结果或抛出异常。
     */
    private static void validateCommand(String command) {
        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException("command must not be blank");
        }
    }
}
