package cn.xiaofuge.deepseek.harness.cases.terminal;

import cn.xiaofuge.deepseek.harness.api.dto.TerminalOpenRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.TerminalSendRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.TerminalSendResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.TerminalSessionResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.terminal.factory.TerminalPipelineFactory;
import cn.xiaofuge.deepseek.harness.cases.terminal.node.TerminalReadNode;
import cn.xiaofuge.deepseek.harness.cases.terminal.node.TerminalSendNode;
import org.springframework.stereotype.Service;

@Service
public class TerminalCommandCaseImpl implements ITerminalCommandCase {
    private final TerminalPipelineFactory terminalPipelineFactory;

    public TerminalCommandCaseImpl(TerminalPipelineFactory terminalPipelineFactory) {
        this.terminalPipelineFactory = terminalPipelineFactory;
    }

    @Override
    public TerminalSessionResponseDTO open(TerminalOpenRequestDTO command) {
        return terminalPipelineFactory.openPipeline().execute(command);
    }

    @Override
    public TerminalSendResponseDTO send(String sessionId, TerminalSendRequestDTO command) {
        return terminalPipelineFactory.sendPipeline().execute(new TerminalSendNode.Input(sessionId, command));
    }

    @Override
    public TerminalSessionResponseDTO close(String sessionId) {
        return terminalPipelineFactory.closePipeline().execute(sessionId);
    }
}
