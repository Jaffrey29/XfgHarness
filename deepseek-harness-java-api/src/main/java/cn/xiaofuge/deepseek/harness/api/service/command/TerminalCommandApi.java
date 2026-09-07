package cn.xiaofuge.deepseek.harness.api.service.command;

import cn.xiaofuge.deepseek.harness.api.dto.terminal.TerminalOpenRequest;
import cn.xiaofuge.deepseek.harness.api.dto.terminal.TerminalSendRequest;
import cn.xiaofuge.deepseek.harness.api.response.terminal.TerminalSendResponse;
import cn.xiaofuge.deepseek.harness.api.response.terminal.TerminalSessionResponse;
import cn.xiaofuge.deepseek.harness.domain.terminal.ITerminalCommandService;
import org.springframework.stereotype.Service;

/**
 * 终端命令适配器。
 * <p>
 * 执行流程：
 * 1. 接收打开、发送、关闭请求；
 * 2. 转交终端领域服务；
 * 3. 返回最新会话状态。
 *
 * <p>调用流程：Controller/DTO → 应用门面 → 用例；响应对象由用例结果转换。</p>
 * <p>示例：DTO 只做协议隔离，不承载领域规则。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class TerminalCommandApi {

    private final ITerminalCommandService terminalCommandService;

    /**
     * 处理终端 命令 API。
     * 流程：转换请求 → 调用用例或领域服务 → 转换响应。
     * @param terminalCommandService 终端 命令 Service参数。
     * @return 方法执行结果。
     */
    public TerminalCommandApi(ITerminalCommandService terminalCommandService) {
        this.terminalCommandService = terminalCommandService;
    }

    /**
     * 打开目标对象。
     * 流程：转换请求 → 调用用例或领域服务 → 转换响应。
     * @param request 请求对象。
     * @return 方法执行结果。
     */
    public TerminalSessionResponse open(TerminalOpenRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("terminal open request must not be null");
        }
        return TerminalSessionResponse.from(terminalCommandService.open(request.name(), request.cwd()));
    }

    /**
     * 发送数据或请求到目标端。
     */
    public TerminalSendResponse send(String sessionId, TerminalSendRequest request) {
        validateSessionId(sessionId);
        if (request == null) {
            throw new IllegalArgumentException("terminal send request must not be null");
        }
        long timeoutMs = request.timeoutMs() == null ? 120_000L : request.timeoutMs();
        return TerminalSendResponse.from(terminalCommandService.send(sessionId, request.command(), timeoutMs));
    }

    /**
     * 释放资源并关闭关联连接。
     */
    public TerminalSessionResponse close(String sessionId) {
        validateSessionId(sessionId);
        return TerminalSessionResponse.from(terminalCommandService.close(sessionId));
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
