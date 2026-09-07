package cn.xiaofuge.deepseek.harness.cases.terminal;

import cn.xiaofuge.deepseek.harness.api.dto.TerminalReadResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.TerminalSessionResponseDTO;
import java.util.List;

public interface ITerminalQueryCase {
    List<TerminalSessionResponseDTO> listSessions();
    TerminalReadResponseDTO read(String sessionId, int maxChars);
}
