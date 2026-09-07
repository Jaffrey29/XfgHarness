package cn.xiaofuge.deepseek.harness.api.service.query;

import cn.xiaofuge.deepseek.harness.api.response.terminal.TerminalReadResponse;
import cn.xiaofuge.deepseek.harness.api.response.terminal.TerminalSessionResponse;
import cn.xiaofuge.deepseek.harness.domain.terminal.ITerminalQueryService;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 终端查询适配器。
 * <p>
 * 执行流程：
 * 1. 接收查询请求；
 * 2. 调用终端领域服务；
 * 3. 返回会话或输出结果。
 *
 * <p>调用流程：Controller/DTO → 应用门面 → 用例；响应对象由用例结果转换。</p>
 * <p>示例：DTO 只做协议隔离，不承载领域规则。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class TerminalQueryApi {

    private final ITerminalQueryService terminalQueryService;

    /**
     * 处理终端 Query API。
     * 流程：转换请求 → 调用用例或领域服务 → 转换响应。
     * @param terminalQueryService 终端 Query Service参数。
     * @return 方法执行结果。
     */
    public TerminalQueryApi(ITerminalQueryService terminalQueryService) {
        this.terminalQueryService = terminalQueryService;
    }

    /**
     * 列出Sessions。
     */
    public List<TerminalSessionResponse> listSessions() {
        return terminalQueryService.list().stream().map(TerminalSessionResponse::from).toList();
    }

    /**
     * 读取目标对象。
     * 流程：转换请求 → 调用用例或领域服务 → 转换响应。
     * @param sessionId 会话 ID。
     * @param maxChars max Chars参数。
     * @return 方法执行结果。
     */
    public TerminalReadResponse read(String sessionId, int maxChars) {
        validateSessionId(sessionId);
        return TerminalReadResponse.from(terminalQueryService.read(sessionId, maxChars));
    }

    /**
     * 校验输入与业务规则；不通过时返回失败结果或抛出异常。
     */
    private static void validateSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
    }
}
