package cn.xiaofuge.deepseek.harness.trigger.service.command;

import cn.xiaofuge.deepseek.harness.api.dto.TerminalOpenRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.TerminalSendRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.TerminalSendResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.TerminalSessionResponseDTO;
import cn.xiaofuge.deepseek.harness.api.ITerminalCommandApi;
import cn.xiaofuge.deepseek.harness.api.response.Response;
import cn.xiaofuge.deepseek.harness.cases.terminal.ITerminalCommandCase;
import org.springframework.stereotype.Service;

@Service
public class TerminalCommandApi implements ITerminalCommandApi {
    private final ITerminalCommandCase terminalCommandCase;

    public TerminalCommandApi(ITerminalCommandCase terminalCommandCase) {
        this.terminalCommandCase = terminalCommandCase;
    }

    public Response<TerminalSessionResponseDTO> open(TerminalOpenRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("terminal open request must not be null");
        }
        return execute(() -> terminalCommandCase.open(request));
    }

    public Response<TerminalSendResponseDTO> send(String sessionId, TerminalSendRequestDTO request) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
        if (request == null) {
            throw new IllegalArgumentException("terminal send request must not be null");
        }
        long timeoutMs = request.timeoutMs() == null ? 120_000L : request.timeoutMs();
        return execute(() -> terminalCommandCase.send(
                sessionId, new TerminalSendRequestDTO(request.command(), timeoutMs)
        ));
    }

    public Response<TerminalSessionResponseDTO> close(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
        return execute(() -> terminalCommandCase.close(sessionId));
    }

    private <T> Response<T> execute(java.util.function.Supplier<T> action) {
        try {
            return Response.success(action.get());
        } catch (IllegalArgumentException exception) {
            return Response.invalidArgument(exception.getMessage());
        } catch (RuntimeException exception) {
            return Response.internalError(exception.getMessage());
        }
    }
}
