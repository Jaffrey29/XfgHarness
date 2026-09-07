package cn.xiaofuge.deepseek.harness.cases.session;

import cn.xiaofuge.deepseek.harness.api.dto.SessionRestoreRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.SessionRestoreResponseDTO;

/**
 * 基于持久化事件日志恢复会话的用例。
 *
 * <p>调用流程：Controller/API → 会话用例 → SessionQueryService/SessionRebuilderService → SessionEventStore。</p>
 * <p>示例：按 sessionId 查询事件后，可重建完整消息时间线。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface ISessionRestoreCase {

    SessionRestoreResponseDTO execute(SessionRestoreRequestDTO request);
}
