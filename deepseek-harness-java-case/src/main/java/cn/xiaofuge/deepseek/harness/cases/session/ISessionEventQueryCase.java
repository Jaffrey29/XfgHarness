package cn.xiaofuge.deepseek.harness.cases.session;

import java.util.List;

/**
 * 会话事件查询用例。
 *
 * <p>调用流程：Controller/API → 会话用例 → SessionQueryService/SessionRebuilderService → SessionEventStore。</p>
 * <p>示例：按 sessionId 查询事件后，可重建完整消息时间线。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface ISessionEventQueryCase {

    List<SessionEventView> queryEvents(SessionEventQuery query);
}
