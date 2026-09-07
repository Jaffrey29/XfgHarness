package cn.xiaofuge.deepseek.harness.cases.terminal.node;

import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.cases.support.CaseGuard;
import cn.xiaofuge.deepseek.harness.api.dto.TerminalSessionResponseDTO;
import cn.xiaofuge.deepseek.harness.domain.terminal.ITerminalCommandService;
import cn.xiaofuge.deepseek.harness.domain.terminal.model.valobj.TerminalSessionSnapshotVO;
import org.springframework.stereotype.Service;

@Service
public class TerminalCloseNode extends AbstractStrategyRouter<String, NoContext, TerminalSessionResponseDTO> {

    private final ITerminalCommandService terminalCommandService;

    public TerminalCloseNode(ITerminalCommandService terminalCommandService) {
        this.terminalCommandService = terminalCommandService;
    }

    @Override
    protected TerminalSessionResponseDTO doApply(String sessionId, NoContext context) {
        CaseGuard.requireSessionId(sessionId);
        return toResponse(terminalCommandService.close(sessionId));
    }

    @Override
    public StrategyHandler<String, NoContext, TerminalSessionResponseDTO> getNext(String sessionId, NoContext context) {
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
