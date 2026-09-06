package cn.walioffice.deepseek.harness.cases.session.restore;

import cn.walioffice.deepseek.harness.api.dto.session.SessionRestoreRequest;
import cn.walioffice.deepseek.harness.api.dto.session.SessionRestoreResponse;

/**
 * 基于持久化事件日志恢复会话的用例。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface ISessionRestoreCase {

    SessionRestoreResponse execute(SessionRestoreRequest request);
}
