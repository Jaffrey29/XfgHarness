package cn.xiaofuge.deepseek.harness.cases.session;

import java.util.List;

/**
 * 会话事件查询用例。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface ISessionEventQueryCase {

    List<SessionEventView> queryEvents(SessionEventQuery query);
}
