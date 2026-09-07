package cn.xiaofuge.deepseek.harness.cases.session;

/**
 * 基于持久化事件日志恢复会话的用例。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface ISessionRestoreCase {

    SessionRestoreResult execute(SessionRestoreCommand command);
}
