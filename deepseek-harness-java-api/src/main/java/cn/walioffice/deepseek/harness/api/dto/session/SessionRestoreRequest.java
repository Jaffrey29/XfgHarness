package cn.walioffice.deepseek.harness.api.dto.session;

/**
 * 会话恢复请求。
 *
 * @param sessionId 需要恢复的会话 ID
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record SessionRestoreRequest(String sessionId) {
}
