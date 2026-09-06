package cn.xiaofuge.deepseek.harness.api.dto.session;

import java.util.List;

/**
 * 会话事件查询请求。
 *
 * @param sessionId 会话 ID
 * @param fromSeq 起始序列号，包含边界；为空表示从头开始
 * @param toSeq 结束序列号，包含边界；为空表示直到末尾
 * @param eventTypes 事件类型过滤；为空表示不过滤
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record SessionEventQueryRequest(
        String sessionId,
        Long fromSeq,
        Long toSeq,
        List<String> eventTypes
) {
}
