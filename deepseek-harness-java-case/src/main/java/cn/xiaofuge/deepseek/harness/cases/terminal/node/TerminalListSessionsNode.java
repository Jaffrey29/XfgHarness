package cn.xiaofuge.deepseek.harness.cases.terminal.node;

import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.api.dto.TerminalSessionResponseDTO;
import cn.xiaofuge.deepseek.harness.domain.terminal.ITerminalQueryService;
import cn.xiaofuge.deepseek.harness.domain.terminal.model.valobj.TerminalSessionSnapshotVO;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TerminalListSessionsNode
        extends AbstractStrategyRouter<Void, NoContext, List<TerminalSessionResponseDTO>> {

    private final ITerminalQueryService terminalQueryService;

    public TerminalListSessionsNode(ITerminalQueryService terminalQueryService) {
        this.terminalQueryService = terminalQueryService;
    }

    @Override
    protected List<TerminalSessionResponseDTO> doApply(Void request, NoContext context) {
        return terminalQueryService.list().stream().map(this::toResponse).toList();
    }

    @Override
    public StrategyHandler<Void, NoContext, List<TerminalSessionResponseDTO>> getNext(Void request, NoContext context) {
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
