package cn.xiaofuge.deepseek.harness.api.dto.session;

/**
 * 会话列表查询请求。
 *
 * @param workspaceId 工作区 ID；为空表示全部工作区
 * @param offset 分页偏移量
 * @param limit 每页数量
 *
 * <p>调用流程：Controller/DTO → 应用门面 → 用例；响应对象由用例结果转换。</p>
 * <p>示例：DTO 只做协议隔离，不承载领域规则。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public record SessionListRequest(
        String workspaceId,
        int offset,
        int limit
) {
}
