package cn.walioffice.deepseek.harness.api.dto.session;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 会话恢复响应。
 *
 * @param sessionId 会话 ID
 * @param restored 是否成功恢复会话
 * @param agentPhase 恢复后的 Agent 阶段，取值为 idle、maintenance、running
 * @param turnCount 已回放的回合数
 * @param stepCount 已回放的步骤数
 * @param lastTurnEndReason 最近一次回合结束原因，可能为空
 * @param messages 投影后的会话消息
 * @param restoredAt 恢复完成时间
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record SessionRestoreResponse(
        String sessionId,
        boolean restored,
        String agentPhase,
        int turnCount,
        int stepCount,
        String lastTurnEndReason,
        List<Map<String, Object>> messages,
        Instant restoredAt
) {
}
