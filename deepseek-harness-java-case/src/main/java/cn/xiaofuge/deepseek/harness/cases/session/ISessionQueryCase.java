package cn.xiaofuge.deepseek.harness.cases.session;

import java.util.List;

/**
 * 会话查询用例。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface ISessionQueryCase {

    List<SessionView> querySessionList(SessionListQuery query);

    SessionView querySessionDetail(SessionDetailQuery query);
}
