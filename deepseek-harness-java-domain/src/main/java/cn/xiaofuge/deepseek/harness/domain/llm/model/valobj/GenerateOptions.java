package cn.xiaofuge.deepseek.harness.domain.llm.model.valobj;

import cn.xiaofuge.deepseek.harness.domain.model.entity.Message;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.ToolSchema;

import java.util.List;
import java.util.Objects;

/**
 * A single model request, fully assembled.
 * <p>
 * Mirrors TS {@code GenerateOptions}. The agent loop builds one of these per
 * model call; adapters translate it into provider wire format.
 *
 * @param provider        registered provider route
 * @param model           model id
 * @param reasoningEffort reasoning effort level (nullable)
 * @param messages        ordered conversation messages (after the system slot)
 * @param system          system prompt text (nullable)
 * @param tools           tool schemas (nullable)
 * @param temperature     sampling temperature (nullable)
 * @param maxTokens       max output tokens (nullable)
 * @param stop            stop sequences (nullable)
 * @param sessionId       session identity for request routing (nullable)
 * @param purpose         auxiliary call purpose: {@code compaction}, {@code session-title}, or null
 * @param baseUrl         per-request upstream base URL (nullable; overrides adapter default)
 * @param apiKey          per-request upstream API key (nullable; overrides adapter default)
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record GenerateOptions(
        String provider,
        String model,
        String reasoningEffort,
        List<Message> messages,
        String system,
        List<ToolSchema> tools,
        Double temperature,
        Integer maxTokens,
        List<String> stop,
        String sessionId,
        String purpose,
        String baseUrl,
        String apiKey
) {
    public GenerateOptions {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(model, "model");
        messages = List.copyOf(messages);
        tools = tools == null ? null : List.copyOf(tools);
        stop = stop == null ? null : List.copyOf(stop);
    }

    /** Extract the call-config portion (provider/model/sampling scalars). */
    public LlmCallConfig toCallConfig() {
        return new LlmCallConfig(provider, model, reasoningEffort, temperature, maxTokens, stop);
    }
}
