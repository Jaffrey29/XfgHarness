package cn.xiaofuge.deepseek.harness.api.command.settings;

/**
 * 承载模型发现所需的上游连接信息。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record DiscoverModelsRequest(
        String baseUrl,
        String apiKeyRef
) {
}
