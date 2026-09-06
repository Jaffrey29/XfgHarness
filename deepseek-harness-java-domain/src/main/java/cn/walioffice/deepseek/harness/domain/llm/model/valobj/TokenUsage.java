package cn.walioffice.deepseek.harness.domain.llm.model.valobj;

/**
 * Token accounting for one model call (cache fields are optional).
 * <p>
 * Counts are DISJOINT: {@code inputTokens} is uncached input only; cached
 * input is reported separately as {@code cacheReadTokens}/{@code cacheWriteTokens}.
 * Mirrors TS {@code TokenUsage}.
 *
 * @param inputTokens       uncached input token count
 * @param outputTokens      output token count
 * @param cacheReadTokens   cache hit tokens (nullable)
 * @param cacheWriteTokens  cache write tokens (nullable)
 * @param reasoningTokens   reasoning/thinking tokens (nullable)
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record TokenUsage(
        int inputTokens,
        int outputTokens,
        Integer cacheReadTokens,
        Integer cacheWriteTokens,
        Integer reasoningTokens
) {
    /** Convenience constructor for just input/output. */
    public TokenUsage(int inputTokens, int outputTokens) {
        this(inputTokens, outputTokens, null, null, null);
    }
}
