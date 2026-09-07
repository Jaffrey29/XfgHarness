package cn.xiaofuge.deepseek.harness.cases.terminal.node;

import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.cases.support.CaseGuard;
import cn.xiaofuge.deepseek.harness.api.dto.TerminalOpenRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.TerminalSessionResponseDTO;
import cn.xiaofuge.deepseek.harness.domain.terminal.ITerminalCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cn.xiaofuge.deepseek.harness.domain.terminal.model.valobj.TerminalSessionSnapshotVO;
import org.springframework.stereotype.Service;

/**
 * 终端打开节点。
 * <p>流程：校验打开命令 → 调用终端领域命令服务创建会话 → 将快照转换为会话 DTO。</p>
 * <p>数据示例：传入 {@code name=build, cwd=/tmp/demo}；
 * 返回 {@code sessionId=term-001, status=RUNNING, createdAt=2026-09-07T10:00:00Z}。</p>
 */
@Service
public class TerminalOpenNode extends AbstractStrategyRouter<TerminalOpenRequestDTO, NoContext, TerminalSessionResponseDTO> {

    private static final Logger log = LoggerFactory.getLogger(TerminalOpenNode.class);

    private final ITerminalCommandService terminalCommandService;

    public TerminalOpenNode(ITerminalCommandService terminalCommandService) {
        this.terminalCommandService = terminalCommandService;
    }

    /**
     * 执行当前节点核心逻辑：校验输入、调用领域服务并返回输出。
     */
    @Override
    protected TerminalSessionResponseDTO doApply(TerminalOpenRequestDTO command, NoContext context) {
        CaseGuard.requireNonNull(command, "terminal open command must not be null");
        var result = terminalCommandService.open(command.name(), command.cwd());
        log.info("[Terminal] 终端打开完成，sessionId={} name={} cwd={}",
                result.sessionId(), command.name(), command.cwd());
        return toResponse(result);
    }

    /**
     * 返回下一执行节点；单节点链路返回默认终止处理。
     */
    @Override
    public StrategyHandler<TerminalOpenRequestDTO, NoContext, TerminalSessionResponseDTO> getNext(
            TerminalOpenRequestDTO command, NoContext context) {
        return defaultStrategyHandler;
    }

    /**
     * 将领域层结果对象转换为用例层 DTO。
     */
    private TerminalSessionResponseDTO toResponse(TerminalSessionSnapshotVO snapshot) {
        return new TerminalSessionResponseDTO(
                snapshot.sessionId(), snapshot.name(), snapshot.cwd(), snapshot.status().name(),
                snapshot.createdAt(), snapshot.updatedAt(), snapshot.exitCode(), snapshot.lastMessage(),
                snapshot.outputChars()
        );
    }
}
