package cn.xiaofuge.deepseek.harness.trigger.http.command;

import cn.xiaofuge.deepseek.harness.api.dto.terminal.TerminalOpenRequest;
import cn.xiaofuge.deepseek.harness.api.dto.terminal.TerminalSendRequest;
import cn.xiaofuge.deepseek.harness.api.response.terminal.TerminalSendResponse;
import cn.xiaofuge.deepseek.harness.api.response.terminal.TerminalSessionResponse;
import cn.xiaofuge.deepseek.harness.api.service.command.TerminalCommandApi;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 终端命令控制器。
 * <p>
 * 执行流程：
 * 1. 接收前端打开或发送命令；
 * 2. 调用终端命令适配器；
 * 3. 返回结果响应。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@RestController
@RequestMapping("/api/harness/terminal")
public class TerminalCommandController {

    private final TerminalCommandApi terminalCommandApi;

    public TerminalCommandController(TerminalCommandApi terminalCommandApi) {
        this.terminalCommandApi = terminalCommandApi;
    }

    @PostMapping("/sessions")
    public TerminalSessionResponse open(@RequestBody TerminalOpenRequest request) {
        return terminalCommandApi.open(request);
    }

    @PostMapping("/sessions/{sessionId}/send")
    public TerminalSendResponse send(@PathVariable("sessionId") String sessionId, @RequestBody TerminalSendRequest request) {
        return terminalCommandApi.send(sessionId, request);
    }

    @PostMapping("/sessions/{sessionId}/close")
    public TerminalSessionResponse close(@PathVariable("sessionId") String sessionId) {
        return terminalCommandApi.close(sessionId);
    }
}
