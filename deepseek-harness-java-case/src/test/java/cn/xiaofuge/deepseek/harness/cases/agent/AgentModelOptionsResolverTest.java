package cn.xiaofuge.deepseek.harness.cases.agent;

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

/**
 * 验证 Agent 模型配置的统一优先级：请求值优先，其次数据库默认配置，最后文件兜底。
 */
class AgentModelOptionsResolverTest {

    /**
     * 请求未指定模型时，必须使用数据库中 deepseek 默认行。
     */
    @Test
    void usesDatabaseDefaultWhenRequestHasNoModel() {
        var settings = new SettingsService(Map.of(
                "deepseek", setting("deepseek", "deepseek", "glm-5.3-flash",
                        "http://db.example/v1", "db-key")
        ));
        var resolver = new AgentModelOptionsResolver(settings, "file-model");

        AgentOptions options = resolver.resolve(command(null, null, null));

        assertEquals("deepseek", options.provider());
        assertEquals("glm-5.3-flash", options.model());
        assertEquals("http://db.example/v1", options.baseUrl());
        assertEquals("db-key", options.apiKey());
    }

    /**
     * 请求指定模型时，优先使用数据库中同名模型的完整配置。
     */
    @Test
    void usesMatchingDatabaseSettingForRequestedModel() {
        var settings = new SettingsService(Map.of(
                "deepseek", setting("deepseek", "deepseek", "glm-5.3-flash",
                        "http://default.example/v1", "default-key"),
                "qwen", setting("qwen", "qwen", "qwen-max",
                        "http://qwen.example/v1", "qwen-key")
        ));
        var resolver = new AgentModelOptionsResolver(settings, "file-model");

        AgentOptions options = resolver.resolve(command(null, "qwen-max", null));

        assertEquals("qwen", options.provider());
        assertEquals("qwen-max", options.model());
        assertEquals("http://qwen.example/v1", options.baseUrl());
        assertEquals("qwen-key", options.apiKey());
    }

    /**
     * 数据库没有可用默认配置时，才使用文件中的模型兜底。
     */
    @Test
    void fallsBackToFileModelWhenDatabaseIsEmpty() {
        var resolver = new AgentModelOptionsResolver(new SettingsService(Map.of()), "file-model");

        AgentOptions options = resolver.resolve(command(null, null, null));

        assertEquals("deepseek", options.provider());
        assertEquals("file-model", options.model());
        assertEquals(null, options.baseUrl());
        assertEquals(null, options.apiKey());
    }

    /**
     * 请求显式 provider 的优先级高于数据库 provider。
     */
    @Test
    void requestProviderOverridesDatabaseProvider() {
        var settings = new SettingsService(Map.of(
                "deepseek", setting("deepseek", "deepseek", "glm-5.3-flash",
                        "http://db.example/v1", "db-key")
        ));
        var resolver = new AgentModelOptionsResolver(settings, "file-model");

        AgentOptions options = resolver.resolve(command("openai", null, null));

        assertEquals("openai", options.provider());
        assertEquals("glm-5.3-flash", options.model());
    }

    /**
     * 构造一条已启用的模型设置。
     */
    private ModelSettingVO setting(String profileCode, String providerCode, String modelCode,
                                   String baseUrl, String apiKeyRef) {
        return new ModelSettingVO(profileCode, providerCode, modelCode, baseUrl, apiKeyRef,
                true, Instant.parse("2026-01-01T00:00:00Z"));
    }

    /**
     * 构造一条消息请求；maxTokens 省略以测试默认值。
     */
    private SendMessageCommand command(String provider, String model, Integer maxTokens) {
        return new SendMessageCommand("agent-1", provider, model, maxTokens, null, "hello", null);
    }

    /**
     * 以 Map 模拟数据库查询服务。
     */
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
