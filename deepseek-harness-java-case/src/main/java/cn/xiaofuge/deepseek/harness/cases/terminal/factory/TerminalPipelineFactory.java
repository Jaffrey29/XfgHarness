package cn.xiaofuge.deepseek.harness.cases.terminal.factory;

import cn.xiaofuge.deepseek.harness.cases.orchestration.CasePipeline;
import cn.xiaofuge.deepseek.harness.cases.orchestration.CasePipelineFactory;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.api.dto.TerminalOpenRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.TerminalReadResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.TerminalSendRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.TerminalSendResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.TerminalSessionResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.terminal.node.TerminalCloseNode;
import cn.xiaofuge.deepseek.harness.cases.terminal.node.TerminalListSessionsNode;
import cn.xiaofuge.deepseek.harness.cases.terminal.node.TerminalOpenNode;
import cn.xiaofuge.deepseek.harness.cases.terminal.node.TerminalReadNode;
import cn.xiaofuge.deepseek.harness.cases.terminal.node.TerminalSendNode;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TerminalPipelineFactory
        implements CasePipelineFactory<TerminalOpenRequestDTO, NoContext, TerminalSessionResponseDTO> {

    private final TerminalOpenNode terminalOpenNode;
    private final TerminalSendNode terminalSendNode;
    private final TerminalCloseNode terminalCloseNode;
    private final TerminalListSessionsNode terminalListSessionsNode;
    private final TerminalReadNode terminalReadNode;

    public TerminalPipelineFactory(
            TerminalOpenNode terminalOpenNode,
            TerminalSendNode terminalSendNode,
            TerminalCloseNode terminalCloseNode,
            TerminalListSessionsNode terminalListSessionsNode,
            TerminalReadNode terminalReadNode
    ) {
        this.terminalOpenNode = terminalOpenNode;
        this.terminalSendNode = terminalSendNode;
        this.terminalCloseNode = terminalCloseNode;
        this.terminalListSessionsNode = terminalListSessionsNode;
        this.terminalReadNode = terminalReadNode;
    }

    public CasePipeline<TerminalOpenRequestDTO, NoContext, TerminalSessionResponseDTO> openPipeline() {
        return CasePipeline.of("Open terminal", terminalOpenNode, () -> NoContext.INSTANCE);
    }

    public CasePipeline<TerminalSendNode.Input, NoContext, TerminalSendResponseDTO> sendPipeline() {
        return CasePipeline.of("Send terminal", terminalSendNode, () -> NoContext.INSTANCE);
    }

    public CasePipeline<String, NoContext, TerminalSessionResponseDTO> closePipeline() {
        return CasePipeline.of("Close terminal", terminalCloseNode, () -> NoContext.INSTANCE);
    }

    public CasePipeline<Void, NoContext, List<TerminalSessionResponseDTO>> listPipeline() {
        return CasePipeline.of("List terminals", terminalListSessionsNode, () -> NoContext.INSTANCE);
    }

    public CasePipeline<TerminalReadNode.Input, NoContext, TerminalReadResponseDTO> readPipeline() {
        return CasePipeline.of("Read terminal", terminalReadNode, () -> NoContext.INSTANCE);
    }

    @Override
    public CasePipeline<TerminalOpenRequestDTO, NoContext, TerminalSessionResponseDTO> pipeline() {
        return openPipeline();
    }
}
