package cn.xiaofuge.deepseek.harness.cases.session;

/**
 * 会话列表查询命令。
 *
 * @param workspaceId 工作区 ID；为空表示全部工作区
 * @param offset 分页偏移量
 * @param limit 每页数量
 *
 * <p>调用流程：Controller/API → 会话用例 → SessionQueryService/SessionRebuilderService → SessionEventStore。</p>
 * <p>示例：按 sessionId 查询事件后，可重建完整消息时间线。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public record SessionListQuery(
        String workspaceId,
        int offset,
        int limit
) {
}
