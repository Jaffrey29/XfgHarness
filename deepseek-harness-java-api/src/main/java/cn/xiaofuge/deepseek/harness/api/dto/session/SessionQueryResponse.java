package cn.xiaofuge.deepseek.harness.api.dto.session;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 会话查询响应。
 *
 * @param sessionId 会话 ID
 * @param workspaceId 工作区 ID
 * @param status 会话状态
 * @param modelCode 解析后的模型编码
 * @param profileCode 执行档位编码
 * @param createdAt 创建时间
 * @param lastActivityAt 最近活动时间
 * @param messages 会话消息，仅详情查询返回
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record SessionQueryResponse(
        String sessionId,
        String workspaceId,
        String status,
        String modelCode,
        String profileCode,
        Instant createdAt,
        Instant lastActivityAt,
        List<Map<String, Object>> messages
) {
}
