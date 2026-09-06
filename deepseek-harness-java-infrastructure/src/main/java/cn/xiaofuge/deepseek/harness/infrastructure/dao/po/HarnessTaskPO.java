package cn.xiaofuge.deepseek.harness.infrastructure.dao.po;

import java.time.Instant;
import java.util.List;

/**
 * 保存一条持久化的 Harness 任务记录。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record HarnessTaskPO(
        String taskId,
        String sessionId,
        String prompt,
        String enabledToolsJson,
        String status,
        Instant submittedAt
) {
}
