package cn.xiaofuge.deepseek.harness.infrastructure.adapter.llm.openai;

import cn.xiaofuge.deepseek.harness.infrastructure.adapter.llm.deepseek.DeepSeekAdapter;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.llm.deepseek.DeepSeekAdapterOptions;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Generic OpenAI-compatible adapter that extends the DeepSeek adapter
 * for any OpenAI-compatible endpoint (OpenAI, Ollama, Groq, Together, etc.).
 * Only overrides the provider identity; request/response format is identical.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class OpenAiCompatibleAdapter extends DeepSeekAdapter {

    private final String providerId;

    /**
     * 处理Open Ai Compatible Adapter相关逻辑。
     */
    public OpenAiCompatibleAdapter(String providerId, DeepSeekAdapterOptions options, ObjectMapper objectMapper) {
        super(options, objectMapper);
        this.providerId = providerId;
    }

    @Override
    public String providerId() {
        return providerId;
    }

    @Override
    public LlmProviderInfo providerInfo(String provider) {
        return new LlmProviderInfo(provider, capitalize(provider));
    }

    /**
     * 处理capitalize相关逻辑。
     */
    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
