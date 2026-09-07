package cn.xiaofuge.deepseek.harness.cases.terminal.node;

import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.cases.support.CaseGuard;
import cn.xiaofuge.deepseek.harness.api.dto.TerminalOpenRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.TerminalSessionResponseDTO;
import cn.xiaofuge.deepseek.harness.domain.terminal.ITerminalCommandService;
import cn.xiaofuge.deepseek.harness.domain.terminal.model.valobj.TerminalSessionSnapshotVO;
import org.springframework.stereotype.Service;

@Service
public class TerminalOpenNode extends AbstractStrategyRouter<TerminalOpenRequestDTO, NoContext, TerminalSessionResponseDTO> {

    private final ITerminalCommandService terminalCommandService;

    public TerminalOpenNode(ITerminalCommandService terminalCommandService) {
        this.terminalCommandService = terminalCommandService;
    }

    @Override
    protected TerminalSessionResponseDTO doApply(TerminalOpenRequestDTO command, NoContext context) {
        CaseGuard.requireNonNull(command, "terminal open command must not be null");
        return toResponse(terminalCommandService.open(command.name(), command.cwd()));
    }

    @Override
    public StrategyHandler<TerminalOpenRequestDTO, NoContext, TerminalSessionResponseDTO> getNext(
            TerminalOpenRequestDTO command, NoContext context) {
        return defaultStrategyHandler;
    }

    private TerminalSessionResponseDTO toResponse(TerminalSessionSnapshotVO snapshot) {
        return new TerminalSessionResponseDTO(
                snapshot.sessionId(), snapshot.name(), snapshot.cwd(), snapshot.status().name(),
                snapshot.createdAt(), snapshot.updatedAt(), snapshot.exitCode(), snapshot.lastMessage(),
                snapshot.outputChars()
        );
    }
}
