package cn.xiaofuge.deepseek.harness.cases.terminal.node;

import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.cases.support.CaseGuard;
import cn.xiaofuge.deepseek.harness.api.dto.TerminalSendRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.TerminalSendResponseDTO;
import cn.xiaofuge.deepseek.harness.domain.terminal.ITerminalCommandService;
import cn.xiaofuge.deepseek.harness.domain.terminal.model.valobj.TerminalSendResultVO;
import org.springframework.stereotype.Service;

@Service
public class TerminalSendNode extends AbstractStrategyRouter<TerminalSendNode.Input, NoContext, TerminalSendResponseDTO> {

    private final ITerminalCommandService terminalCommandService;

    public TerminalSendNode(ITerminalCommandService terminalCommandService) {
        this.terminalCommandService = terminalCommandService;
    }

    @Override
    protected TerminalSendResponseDTO doApply(Input input, NoContext context) {
        CaseGuard.requireNonNull(input.request(), "terminal send command must not be null");
        CaseGuard.requireSessionId(input.sessionId());
        return toResponse(terminalCommandService.send(
                input.sessionId(), input.request().command(), input.request().timeoutMs()
        ));
    }

    @Override
    public StrategyHandler<Input, NoContext, TerminalSendResponseDTO> getNext(
            Input input, NoContext context) {
        return defaultStrategyHandler;
    }

    public record Input(String sessionId, TerminalSendRequestDTO request) {
    }

    private TerminalSendResponseDTO toResponse(TerminalSendResultVO result) {
        return new TerminalSendResponseDTO(
                result.sessionId(), result.command(), result.output(), result.completed(),
                result.truncated(), result.status().name(), result.exitCode(),
                result.durationMs(), result.completedAt()
        );
    }
}
