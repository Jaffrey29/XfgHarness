package cn.walioffice.deepseek.harness.trigger.http.query;

import cn.walioffice.deepseek.harness.api.response.terminal.TerminalReadResponse;
import cn.walioffice.deepseek.harness.api.response.terminal.TerminalSessionResponse;
import cn.walioffice.deepseek.harness.api.service.query.TerminalQueryApi;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 终端查询控制器。
 * <p>
 * 执行流程：
 * 1. 接收查询请求；
 * 2. 调用终端查询适配器；
 * 3. 返回会话列表或输出片段。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@RestController
@RequestMapping("/api/harness/terminal")
public class TerminalQueryController {

    private final TerminalQueryApi terminalQueryApi;

    public TerminalQueryController(TerminalQueryApi terminalQueryApi) {
        this.terminalQueryApi = terminalQueryApi;
    }

    @GetMapping("/sessions")
    public List<TerminalSessionResponse> listSessions() {
        return terminalQueryApi.listSessions();
    }

    @GetMapping("/sessions/{sessionId}/read")
    public TerminalReadResponse read(@PathVariable("sessionId") String sessionId,
                                     @RequestParam(name = "maxChars", defaultValue = "8000") int maxChars) {
        return terminalQueryApi.read(sessionId, maxChars);
    }
}
