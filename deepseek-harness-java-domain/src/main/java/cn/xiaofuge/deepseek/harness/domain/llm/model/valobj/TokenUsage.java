package cn.xiaofuge.deepseek.harness.domain.llm.model.valobj;

/**
 * TokenUsage 表示TokenUsage。
 */
public record TokenUsage(
        int inputTokens,
        int outputTokens,
        Integer cacheReadTokens,
        Integer cacheWriteTokens,
        Integer reasoningTokens
) {
    /**
     * 构造TokenUsage领域对象。
     * <p>流程：校验入参 → 执行领域动作 → 返回处理结果。</p>
     * @param inputTokens inputTokensToken列表。
     */
    public TokenUsage(int inputTokens, int outputTokens) {
        this(inputTokens, outputTokens, null, null, null);
    }
}
