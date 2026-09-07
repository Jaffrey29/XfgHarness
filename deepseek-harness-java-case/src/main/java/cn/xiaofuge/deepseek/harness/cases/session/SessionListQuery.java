package cn.xiaofuge.deepseek.harness.cases.session;

/**
 * 会话列表查询命令。
 *
 * @param workspaceId 工作区 ID；为空表示全部工作区
 * @param offset 分页偏移量
 * @param limit 每页数量
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record SessionListQuery(
        String workspaceId,
        int offset,
        int limit
) {
}
