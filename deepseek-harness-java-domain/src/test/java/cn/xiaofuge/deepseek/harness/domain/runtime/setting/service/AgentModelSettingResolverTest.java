package cn.xiaofuge.deepseek.harness.domain.runtime.setting.service;

import cn.xiaofuge.deepseek.harness.domain.agent.model.entity.AgentOptions;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.IModelSettingQueryService;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.model.valobj.ModelSettingVO;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class AgentModelSettingResolverTest {

    @Test
    void usesDatabaseDefaultWhenRequestHasNoModel() {
        var settings = new SettingsService(Map.of(
                "deepseek", setting("deepseek", "deepseek", "glm-5.3-flash",
                        "http://db.example/v1", "db-key")
        ));
        var resolver = new AgentModelSettingResolver(settings, "file-model");

        AgentOptions options = resolver.resolve(null, null, null);

        assertEquals("deepseek", options.provider());
        assertEquals("glm-5.3-flash", options.model());
        assertEquals("http://db.example/v1", options.baseUrl());
        assertEquals("db-key", options.apiKey());
    }

    @Test
    void usesMatchingDatabaseSettingForRequestedModel() {
        var settings = new SettingsService(Map.of(
                "deepseek", setting("deepseek", "deepseek", "glm-5.3-flash",
                        "http://default.example/v1", "default-key"),
                "qwen", setting("qwen", "qwen", "qwen-max",
                        "http://qwen.example/v1", "qwen-key")
        ));
        var resolver = new AgentModelSettingResolver(settings, "file-model");

        AgentOptions options = resolver.resolve(null, "qwen-max", null);

        assertEquals("qwen", options.provider());
        assertEquals("qwen-max", options.model());
        assertEquals("http://qwen.example/v1", options.baseUrl());
        assertEquals("qwen-key", options.apiKey());
    }

    @Test
    void fallsBackToFileModelWhenDatabaseIsEmpty() {
        var resolver = new AgentModelSettingResolver(new SettingsService(Map.of()), "file-model");

        AgentOptions options = resolver.resolve(null, null, null);

        assertEquals("deepseek", options.provider());
        assertEquals("file-model", options.model());
        assertEquals(8192, options.maxTokens());
    }

    @Test
    void requestProviderOverridesDatabaseProvider() {
        var settings = new SettingsService(Map.of(
                "deepseek", setting("deepseek", "deepseek", "glm-5.3-flash",
                        "http://db.example/v1", "db-key")
        ));
        var resolver = new AgentModelSettingResolver(settings, "file-model");

        AgentOptions options = resolver.resolve("openai", null, null);

        assertEquals("openai", options.provider());
        assertEquals("glm-5.3-flash", options.model());
    }

    private ModelSettingVO setting(String profileCode, String providerCode, String modelCode,
                                   String baseUrl, String apiKeyRef) {
        return new ModelSettingVO(profileCode, providerCode, modelCode, baseUrl, apiKeyRef,
                null, true, Instant.parse("2026-01-01T00:00:00Z"));
    }

    private static final class SettingsService implements IModelSettingQueryService {
        private final Map<String, ModelSettingVO> settings;

        private SettingsService(Map<String, ModelSettingVO> settings) {
            this.settings = new HashMap<>(settings);
        }

        @Override
        public List<ModelSettingVO> queryAll() {
            return List.copyOf(settings.values());
        }

        @Override
        public Optional<ModelSettingVO> findByProfileCode(String profileCode) {
            return Optional.ofNullable(settings.get(profileCode));
        }
    }
}
