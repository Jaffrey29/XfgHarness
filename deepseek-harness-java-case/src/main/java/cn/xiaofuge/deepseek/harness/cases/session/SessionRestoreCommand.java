package cn.xiaofuge.deepseek.harness.cases.session;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 会话恢复命令。
 *
 * @param sessionId 需要恢复的会话 ID
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record SessionRestoreCommand(String sessionId) {
}
