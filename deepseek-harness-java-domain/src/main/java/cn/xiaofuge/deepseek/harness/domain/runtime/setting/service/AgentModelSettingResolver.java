package cn.xiaofuge.deepseek.harness.domain.runtime.setting.service;

import cn.xiaofuge.deepseek.harness.domain.agent.model.entity.AgentOptions;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.IModelSettingQueryService;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.model.valobj.ModelSettingVO;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 解析 Agent 运行模型。
 * <p>优先级：请求显式配置 → 数据库全局默认配置 → 配置化的文件兜底模型。
 * 该规则属于模型运行领域，不依赖接口层或用例层。</p>
 */
@Service
public class AgentModelSettingResolver {

    private static final Logger log = LoggerFactory.getLogger(AgentModelSettingResolver.class);

    private static final String DEFAULT_MODEL_PROFILE = "deepseek";
    private static final String DEFAULT_PROVIDER = "deepseek";
    private static final Integer DEFAULT_MAX_TOKENS = 8192;

    private final IModelSettingQueryService modelSettingQueryService;
    private final String fallbackModel;

    public AgentModelSettingResolver(
            IModelSettingQueryService modelSettingQueryService,
            @Value("${harness.llm.deepseek.default-model:gpt-5.5}") String fallbackModel
    ) {
        this.modelSettingQueryService = modelSettingQueryService;
        this.fallbackModel = fallbackModel;
    }

    public AgentOptions resolve(String requestedProvider, String requestedModel, Integer requestedMaxTokens) {
        ModelSettingVO requestedSetting = hasText(requestedModel)
                ? findByModel(requestedModel).orElse(null)
                : null;
        ModelSettingVO defaultSetting = defaultSetting().orElse(null);
        ModelSettingVO setting = requestedSetting != null ? requestedSetting : defaultSetting;

        String provider = firstNonBlank(
                requestedProvider,
                setting == null ? null : setting.providerCode(),
                DEFAULT_PROVIDER
        );
        String model = firstNonBlank(
                requestedModel,
                setting == null ? null : setting.modelCode(),
                fallbackModel
        );

        logResolutionGuidance(requestedModel, requestedSetting, defaultSetting);
        return new AgentOptions(
                provider,
                model,
                requestedMaxTokens != null ? requestedMaxTokens : DEFAULT_MAX_TOKENS,
                setting == null ? null : setting.baseUrl(),
                setting == null ? null : setting.apiKeyRef()
        );
    }

    private Optional<ModelSettingVO> defaultSetting() {
        return modelSettingQueryService.findByProfileCode(DEFAULT_MODEL_PROFILE)
                .filter(ModelSettingVO::enabled);
    }

    private Optional<ModelSettingVO> findByModel(String modelCode) {
        return modelSettingQueryService.queryAll().stream()
                .filter(ModelSettingVO::enabled)
                .filter(setting -> modelCode.equals(setting.modelCode()))
                .findFirst();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private void logResolutionGuidance(
            String requestedModel,
            ModelSettingVO requestedSetting,
            ModelSettingVO defaultSetting
    ) {
        if (hasText(requestedModel) && requestedSetting == null) {
            if (defaultSetting == null) {
                log.warn("请求模型 '{}' 在数据库中无启用配置，且默认模型也未配置；已使用文件兜底 model={}。",
                        requestedModel, fallbackModel);
            } else {
                log.warn("请求模型 '{}' 在数据库中无启用配置，已回退到默认模型 '{}={}'。",
                        requestedModel, defaultSetting.providerCode(), defaultSetting.modelCode());
            }
        } else if (!hasText(requestedModel) && defaultSetting == null) {
            log.warn("数据库中不存在启用的默认模型配置 profileCode='{}'；已使用文件兜底 model={}。",
                    DEFAULT_MODEL_PROFILE, fallbackModel);
        }

        ModelSettingVO effectiveSetting = requestedSetting != null ? requestedSetting : defaultSetting;
        if (effectiveSetting == null) {
            return;
        }
        if (!hasText(effectiveSetting.baseUrl())) {
            log.warn("模型 '{}={}' 的 baseUrl 为空，将依赖 LLM 适配器内置兜底地址。",
                    effectiveSetting.providerCode(), effectiveSetting.modelCode());
        }
        if (!hasText(effectiveSetting.apiKeyRef())) {
            log.warn("模型 '{}={}' 的 apiKey 为空，将依赖 LLM 适配器内置兜底密钥。",
                    effectiveSetting.providerCode(), effectiveSetting.modelCode());
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
