package cn.xiaofuge.deepseek.harness.domain.llm.model.valobj;

import java.util.List;
import java.util.Objects;

/**
 * Provider, model, reasoning effort, and sampling scalars of one conversation's
 * requests. Every field maps 1:1 onto {@link GenerateOptions}.
 * <p>
 * Mirrors TS {@code LlmCallConfig}.
 *
 * @param provider        registered provider route
 * @param model           model id
 * @param reasoningEffort reasoning effort level (nullable)
 * @param temperature     sampling temperature (nullable)
 * @param maxTokens       max output tokens (nullable)
 * @param stop            stop sequences (nullable, empty list means none)
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record LlmCallConfig(
        String provider,
        String model,
        String reasoningEffort,
        Double temperature,
        Integer maxTokens,
        List<String> stop
) {
    public LlmCallConfig {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(model, "model");
        stop = stop == null ? null : List.copyOf(stop);
    }

    /**
     * Field-wise equality — the comparison a caller runs to decide whether
     * a proposed configuration is a real change or the held one restated.
     */
    public boolean configEquals(LlmCallConfig other) {
        if (other == null) return false;
        if (!provider.equals(other.provider)) return false;
        if (!model.equals(other.model)) return false;
        if (!Objects.equals(reasoningEffort, other.reasoningEffort)) return false;
        if (!Objects.equals(temperature, other.temperature)) return false;
        if (!Objects.equals(maxTokens, other.maxTokens)) return false;
        if (stop == null || other.stop == null) return stop == other.stop;
        if (stop.size() != other.stop.size()) return false;
        for (int i = 0; i < stop.size(); i++) {
            if (!stop.get(i).equals(other.stop.get(i))) return false;
        }
        return true;
    }
}
