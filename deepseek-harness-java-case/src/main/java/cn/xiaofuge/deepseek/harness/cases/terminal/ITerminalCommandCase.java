package cn.xiaofuge.deepseek.harness.cases.terminal;

import cn.xiaofuge.deepseek.harness.api.dto.TerminalOpenRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.TerminalSendRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.TerminalSendResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.TerminalSessionResponseDTO;

public interface ITerminalCommandCase {
    TerminalSessionResponseDTO open(TerminalOpenRequestDTO command);
    TerminalSendResponseDTO send(String sessionId, TerminalSendRequestDTO command);
    TerminalSessionResponseDTO close(String sessionId);
}
