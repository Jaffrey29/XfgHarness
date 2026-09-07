package cn.xiaofuge.deepseek.harness.api;

import cn.xiaofuge.deepseek.harness.api.dto.TerminalReadResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.TerminalSessionResponseDTO;
import cn.xiaofuge.deepseek.harness.api.response.Response;
import java.util.List;

public interface ITerminalQueryApi {

    Response<List<TerminalSessionResponseDTO>> listSessions();

    Response<TerminalReadResponseDTO> read(String sessionId, int maxChars);
}
