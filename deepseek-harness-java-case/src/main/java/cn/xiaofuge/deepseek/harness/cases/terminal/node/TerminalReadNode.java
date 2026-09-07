package cn.xiaofuge.deepseek.harness.cases.terminal.node;

import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.cases.support.CaseGuard;
import cn.xiaofuge.deepseek.harness.api.dto.TerminalReadResponseDTO;
import cn.xiaofuge.deepseek.harness.domain.terminal.ITerminalQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cn.xiaofuge.deepseek.harness.domain.terminal.model.valobj.TerminalReadResultVO;
import org.springframework.stereotype.Service;

/**
 * 终端输出读取节点。
 * <p>流程：校验 sessionId → 按最大字符数调用终端领域查询服务 → 将读取结果转换为输出 DTO。</p>
 * <p>数据示例：传入 {@code sessionId=term-001, maxChars=8000}；
 * 返回 {@code content="total 0", truncated=false, status=COMPLETED}。</p>
 */
@Service
public class TerminalReadNode
        extends AbstractStrategyRouter<TerminalReadNode.Input, NoContext, TerminalReadResponseDTO> {

    private static final Logger log = LoggerFactory.getLogger(TerminalReadNode.class);

    private final ITerminalQueryService terminalQueryService;

    public TerminalReadNode(ITerminalQueryService terminalQueryService) {
        this.terminalQueryService = terminalQueryService;
    }

    /**
     * 执行当前节点核心逻辑：校验输入、调用领域服务并返回输出。
     */
    @Override
    protected TerminalReadResponseDTO doApply(Input input, NoContext context) {
        CaseGuard.requireSessionId(input.sessionId());
        var result = terminalQueryService.read(input.sessionId(), input.maxChars());
        log.debug("[Terminal] 终端输出读取完成，sessionId={} maxChars={} truncated={}",
                input.sessionId(), input.maxChars(), result.truncated());
        return toResponse(result);
    }

    /**
     * 返回下一执行节点；单节点链路返回默认终止处理。
     */
    @Override
    public StrategyHandler<Input, NoContext, TerminalReadResponseDTO> getNext(Input input, NoContext context) {
        return defaultStrategyHandler;
    }

    public record Input(String sessionId, int maxChars) {
    }

    /**
     * 将领域层结果对象转换为用例层 DTO。
     */
    private TerminalReadResponseDTO toResponse(TerminalReadResultVO result) {
        return new TerminalReadResponseDTO(
                result.sessionId(), result.content(), result.truncated(),
                result.status().name(), result.exitCode()
        );
    }
}
