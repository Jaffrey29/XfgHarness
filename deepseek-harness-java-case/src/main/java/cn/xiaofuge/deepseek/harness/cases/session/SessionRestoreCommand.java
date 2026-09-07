package cn.xiaofuge.deepseek.harness.cases.session;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 会话恢复命令。
 *
 * @param sessionId 需要恢复的会话 ID
 *
 * <p>调用流程：Controller/API → 会话用例 → SessionQueryService/SessionRebuilderService → SessionEventStore。</p>
 * <p>示例：按 sessionId 查询事件后，可重建完整消息时间线。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public record SessionRestoreCommand(String sessionId) {
}
