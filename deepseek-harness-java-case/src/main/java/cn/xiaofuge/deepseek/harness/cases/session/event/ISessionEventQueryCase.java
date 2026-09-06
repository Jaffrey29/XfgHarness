package cn.xiaofuge.deepseek.harness.cases.session.event;

import cn.xiaofuge.deepseek.harness.api.dto.session.SessionEventQueryRequest;
import cn.xiaofuge.deepseek.harness.api.dto.session.SessionEventQueryResponse;

import java.util.List;

/**
 * 会话事件查询用例。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface ISessionEventQueryCase {

    List<SessionEventQueryResponse> queryEvents(SessionEventQueryRequest request);
}
