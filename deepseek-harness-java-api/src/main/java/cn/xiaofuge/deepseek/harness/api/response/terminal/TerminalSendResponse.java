package cn.xiaofuge.deepseek.harness.api.response.terminal;

import cn.xiaofuge.deepseek.harness.domain.terminal.model.valobj.TerminalSendResultVO;
import java.time.Instant;

/**
 * 终端命令结果响应。
 * <p>
 * 执行流程：
 * 1. 读取命令执行结果；
 * 2. 映射为前端结构；
 * 3. 返回输出与终端状态。
 *
 * <p>调用流程：Controller/DTO → 应用门面 → 用例；响应对象由用例结果转换。</p>
 * <p>示例：DTO 只做协议隔离，不承载领域规则。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public record TerminalSendResponse(
        String sessionId,
        String command,
        String output,
        boolean completed,
        boolean truncated,
        String status,
        Integer exitCode,
        long durationMs,
        Instant completedAt
) {

    /**
     * 根据目标对象。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param result 结果对象。
     * @return 创建的领域对象。
     */
    public static TerminalSendResponse from(TerminalSendResultVO result) {
        return new TerminalSendResponse(
                result.sessionId(),
                result.command(),
                result.output(),
                result.completed(),
                result.truncated(),
                result.status().name(),
                result.exitCode(),
                result.durationMs(),
                result.completedAt()
        );
    }
}
