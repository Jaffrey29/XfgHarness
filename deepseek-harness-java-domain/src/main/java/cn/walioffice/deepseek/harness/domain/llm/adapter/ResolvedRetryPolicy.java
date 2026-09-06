package cn.walioffice.deepseek.harness.domain.llm.adapter;

import java.util.List;

/**
 * Fully resolved retry policy for one provider route.
 * <p>
 * Mirrors TS {@code ResolvedRetryPolicy}. Two modes:
 * <ul>
 *   <li>{@code normal} — bounded exponential backoff with jitter, max retries</li>
 *   <li>{@code always} — unbounded retry until success or cancellation</li>
 * </ul>
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public sealed interface ResolvedRetryPolicy permits
        ResolvedRetryPolicy.Normal,
        ResolvedRetryPolicy.Always {

    int initialDelayMs();
    int maxDelayMs();
    double jitterRatio();

    /** Bounded transient retry policy. */
    record Normal(
            int initialDelayMs,
            int maxDelayMs,
            double jitterRatio,
            int maxRetries,
            List<String> retryableCodes
    ) implements ResolvedRetryPolicy {
        public Normal {
            retryableCodes = List.copyOf(retryableCodes);
        }
    }

    /** Unbounded retry policy. */
    record Always(
            int initialDelayMs,
            int maxDelayMs,
            double jitterRatio
    ) implements ResolvedRetryPolicy {}
}
