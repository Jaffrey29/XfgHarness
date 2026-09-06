package cn.walioffice.deepseek.harness.infrastructure.adapter.llm.deepseek;

import java.util.Optional;

/**
 * DeepSeek 适配器对外暴露的一个可选模型目录项。
 * <p>
 * 对应 TS 侧 {@code DeepSeekCatalogModel}。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record DirectSeekCatalogModel(
        String id,
        String name,
        String description,
        Integer contextWindow,
        Integer maxTokens
) {
    public DirectSeekCatalogModel {
        java.util.Objects.requireNonNull(id, "id");
    }

    public Optional<String> nameOpt() { return Optional.ofNullable(name); }
    public Optional<String> descriptionOpt() { return Optional.ofNullable(description); }
    public Optional<Integer> contextWindowOpt() { return Optional.ofNullable(contextWindow); }
    public Optional<Integer> maxTokensOpt() { return Optional.ofNullable(maxTokens); }
}
