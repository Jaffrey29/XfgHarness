package cn.xiaofuge.deepseek.harness.api.response.terminal;

import cn.xiaofuge.deepseek.harness.domain.terminal.model.valobj.TerminalSessionSnapshotVO;
import java.time.Instant;

/**
 * 终端会话响应。
 * <p>
 * 执行流程：
 * 1. 读取领域快照；
 * 2. 映射为前端可见字段；
 * 3. 返回会话状态与输出摘要。
 *
 * <p>调用流程：Controller/DTO → 应用门面 → 用例；响应对象由用例结果转换。</p>
 * <p>示例：DTO 只做协议隔离，不承载领域规则。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public record TerminalSessionResponse(
        String sessionId,
        String name,
        String cwd,
        String status,
        Instant createdAt,
        Instant updatedAt,
        Integer exitCode,
        String lastMessage,
        int outputChars
) {

    /**
     * 根据目标对象。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param snapshot 快照对象。
     * @return 创建的领域对象。
     */
    public static TerminalSessionResponse from(TerminalSessionSnapshotVO snapshot) {
        return new TerminalSessionResponse(
                snapshot.sessionId(),
                snapshot.name(),
                snapshot.cwd(),
                snapshot.status().name(),
                snapshot.createdAt(),
                snapshot.updatedAt(),
                snapshot.exitCode(),
                snapshot.lastMessage(),
                snapshot.outputChars()
        );
    }
}
