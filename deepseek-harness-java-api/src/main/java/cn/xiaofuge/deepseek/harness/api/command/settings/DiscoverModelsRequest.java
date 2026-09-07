package cn.xiaofuge.deepseek.harness.api.command.settings;

/**
 * 承载模型发现所需的上游连接信息。
 *
 * <p>调用流程：Controller/DTO → 应用门面 → 用例；响应对象由用例结果转换。</p>
 * <p>示例：DTO 只做协议隔离，不承载领域规则。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public record DiscoverModelsRequest(
        String baseUrl,
        String apiKeyRef
) {
}
