package cn.xiaofuge.deepseek.harness.cases.terminal;

import cn.xiaofuge.deepseek.harness.api.dto.TerminalReadResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.TerminalSessionResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.terminal.factory.TerminalPipelineFactory;
import cn.xiaofuge.deepseek.harness.cases.terminal.node.TerminalReadNode;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TerminalQueryCaseImpl implements ITerminalQueryCase {
    private final TerminalPipelineFactory terminalPipelineFactory;

    public TerminalQueryCaseImpl(TerminalPipelineFactory terminalPipelineFactory) {
        this.terminalPipelineFactory = terminalPipelineFactory;
    }

    @Override
    public List<TerminalSessionResponseDTO> listSessions() {
        return terminalPipelineFactory.listPipeline().execute(null);
    }

    @Override
    public TerminalReadResponseDTO read(String sessionId, int maxChars) {
        return terminalPipelineFactory.readPipeline().execute(new TerminalReadNode.Input(sessionId, maxChars));
    }
}
