package cn.walioffice.deepseek.harness.api.response.terminal;

import cn.walioffice.deepseek.harness.domain.terminal.model.valobj.TerminalReadResultVO;

/**
 * 终端读取响应。
 * <p>
 * 执行流程：
 * 1. 读取当前输出片段；
 * 2. 映射为响应体；
 * 3. 返回给前端轮询。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record TerminalReadResponse(
        String sessionId,
        String content,
        boolean truncated,
        String status,
        Integer exitCode
) {

    public static TerminalReadResponse from(TerminalReadResultVO result) {
        return new TerminalReadResponse(
                result.sessionId(),
                result.content(),
                result.truncated(),
                result.status().name(),
                result.exitCode()
        );
    }
}
