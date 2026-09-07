package cn.xiaofuge.deepseek.harness.cases.terminal.node;

import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.cases.support.CaseGuard;
import cn.xiaofuge.deepseek.harness.api.dto.TerminalReadResponseDTO;
import cn.xiaofuge.deepseek.harness.domain.terminal.ITerminalQueryService;
import cn.xiaofuge.deepseek.harness.domain.terminal.model.valobj.TerminalReadResultVO;
import org.springframework.stereotype.Service;

@Service
public class TerminalReadNode
        extends AbstractStrategyRouter<TerminalReadNode.Input, NoContext, TerminalReadResponseDTO> {

    private final ITerminalQueryService terminalQueryService;

    public TerminalReadNode(ITerminalQueryService terminalQueryService) {
        this.terminalQueryService = terminalQueryService;
    }

    @Override
    protected TerminalReadResponseDTO doApply(Input input, NoContext context) {
        CaseGuard.requireSessionId(input.sessionId());
        return toResponse(terminalQueryService.read(input.sessionId(), input.maxChars()));
    }

    @Override
    public StrategyHandler<Input, NoContext, TerminalReadResponseDTO> getNext(Input input, NoContext context) {
        return defaultStrategyHandler;
    }

    public record Input(String sessionId, int maxChars) {
    }

    private TerminalReadResponseDTO toResponse(TerminalReadResultVO result) {
        return new TerminalReadResponseDTO(
                result.sessionId(), result.content(), result.truncated(),
                result.status().name(), result.exitCode()
        );
    }
}
