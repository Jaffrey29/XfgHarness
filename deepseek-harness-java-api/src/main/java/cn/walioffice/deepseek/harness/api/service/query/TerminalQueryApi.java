package cn.walioffice.deepseek.harness.api.service.query;

import cn.walioffice.deepseek.harness.api.response.terminal.TerminalReadResponse;
import cn.walioffice.deepseek.harness.api.response.terminal.TerminalSessionResponse;
import cn.walioffice.deepseek.harness.domain.terminal.ITerminalQueryService;
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
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class TerminalQueryApi {

    private final ITerminalQueryService terminalQueryService;

    public TerminalQueryApi(ITerminalQueryService terminalQueryService) {
        this.terminalQueryService = terminalQueryService;
    }

    public List<TerminalSessionResponse> listSessions() {
        return terminalQueryService.list().stream().map(TerminalSessionResponse::from).toList();
    }

    public TerminalReadResponse read(String sessionId, int maxChars) {
        validateSessionId(sessionId);
        return TerminalReadResponse.from(terminalQueryService.read(sessionId, maxChars));
    }

    private static void validateSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
    }
}
