package cn.xiaofuge.deepseek.harness.infrastructure.adapter.llm.deepseek;

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

    /**
     * 处理name Opt相关逻辑。
     */
    public Optional<String> nameOpt() { return Optional.ofNullable(name); }
    /**
     * 处理description Opt相关逻辑。
     */
    public Optional<String> descriptionOpt() { return Optional.ofNullable(description); }
    /**
     * 处理context Window Opt相关逻辑。
     */
    public Optional<Integer> contextWindowOpt() { return Optional.ofNullable(contextWindow); }
    /**
     * 处理max Tokens Opt相关逻辑。
     */
    public Optional<Integer> maxTokensOpt() { return Optional.ofNullable(maxTokens); }
}
