package cn.walioffice.deepseek.harness.cases.session.query;

import cn.walioffice.deepseek.harness.api.dto.session.SessionDetailRequest;
import cn.walioffice.deepseek.harness.api.dto.session.SessionListRequest;
import cn.walioffice.deepseek.harness.api.dto.session.SessionQueryResponse;

import java.util.List;

/**
 * 会话查询用例。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface ISessionQueryCase {

    List<SessionQueryResponse> querySessionList(SessionListRequest request);

    SessionQueryResponse querySessionDetail(SessionDetailRequest request);
}
