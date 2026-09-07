package cn.xiaofuge.deepseek.harness.domain.llm.model.valobj;

import cn.xiaofuge.deepseek.harness.domain.model.entity.Message;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.ToolSchema;

import java.util.List;
import java.util.Objects;

/**
 * GenerateOptions 表示GenerateOptions智能体选项。
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

    /**
     * 处理toCallConfig。
     * @return 领域处理结果。
     */
    public LlmCallConfig toCallConfig() {
        return new LlmCallConfig(provider, model, reasoningEffort, temperature, maxTokens, stop);
    }
}
