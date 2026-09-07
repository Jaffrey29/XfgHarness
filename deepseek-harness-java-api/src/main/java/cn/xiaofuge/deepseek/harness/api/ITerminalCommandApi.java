package cn.xiaofuge.deepseek.harness.api;

import cn.xiaofuge.deepseek.harness.api.dto.TerminalOpenRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.TerminalSendRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.TerminalSendResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.TerminalSessionResponseDTO;
import cn.xiaofuge.deepseek.harness.api.response.Response;

public interface ITerminalCommandApi {

    Response<TerminalSessionResponseDTO> open(TerminalOpenRequestDTO request);

    Response<TerminalSendResponseDTO> send(String sessionId, TerminalSendRequestDTO request);

    Response<TerminalSessionResponseDTO> close(String sessionId);
}
