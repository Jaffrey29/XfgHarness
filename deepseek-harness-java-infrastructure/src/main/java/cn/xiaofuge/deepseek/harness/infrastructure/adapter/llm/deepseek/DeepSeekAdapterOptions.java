package cn.xiaofuge.deepseek.harness.infrastructure.adapter.llm.deepseek;

import cn.xiaofuge.deepseek.harness.domain.llm.adapter.ResolvedRetryPolicy;
import java.util.List;

/**
 * {@link DeepSeekAdapter} 的配置项。
 * <p>
 * 对应 TS 侧 {@code DeepSeekConnectionOptions} 和 {@code DeepSeekAdapterOptions}。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record DeepSeekAdapterOptions(
        String baseUrl,
        String apiKey,
        int maxTokens,
        int defaultContextWindow,
        int streamIdleTimeoutMs,
        List<DirectSeekCatalogModel> models,
        ResolvedRetryPolicy retryPolicy
) {
    public DeepSeekAdapterOptions {
        java.util.Objects.requireNonNull(baseUrl, "baseUrl");
        java.util.Objects.requireNonNull(apiKey, "apiKey");
        models = List.copyOf(models);
    }

    /**
     * 解析e Api Key。
     */
    public String resolveApiKey() {
        return apiKey;
    }
}
