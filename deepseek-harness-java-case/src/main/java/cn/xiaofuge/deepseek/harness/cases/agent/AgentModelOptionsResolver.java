package cn.xiaofuge.deepseek.harness.cases.agent;

import cn.xiaofuge.deepseek.harness.cases.agent.SendMessageCommand;
import cn.xiaofuge.deepseek.harness.domain.agent.model.entity.AgentOptions;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.IModelSettingQueryService;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.model.valobj.ModelSettingVO;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 统一解析 Agent 请求使用的模型配置。
 * <p>解析优先级为：请求显式 provider/model -> 数据库中的模型设置 -> application.yml 兜底。
 * 数据库中 profileCode=deepseek 的启用记录表示全局默认模型；
 * 当请求显式指定 model 时，按 modelCode 在数据库中查找对应连接配置。
 * <p>数据示例：数据库 deepseek 行为 glm-5.3-flash，请求未指定 model 时使用该行；
 * 请求 model=qwen-max 且存在同名启用行，则使用该行的 provider/baseUrl/apiKeyRef。
 */
@Service
public class AgentModelOptionsResolver {

    private static final Logger log = LoggerFactory.getLogger(AgentModelOptionsResolver.class);

    private static final String DEFAULT_MODEL_PROFILE = "deepseek";
    private static final String DEFAULT_PROVIDER = "deepseek";

    private final IModelSettingQueryService modelSettingQueryService;
    private final String fallbackModel;

    /**
     * 创建模型配置解析器。
     *
     * @param modelSettingQueryService 数据库模型设置查询服务
     * @param fallbackModel 数据库无可用配置时的文件兜底模型
     */
    public AgentModelOptionsResolver(
            IModelSettingQueryService modelSettingQueryService,
            @Value("${harness.llm.deepseek.default-model:gpt-5.5}") String fallbackModel
    ) {
        this.modelSettingQueryService = modelSettingQueryService;
        this.fallbackModel = fallbackModel;
    }

    /**
     * 将请求和持久化模型设置解析为 Agent 运行参数。
     * <p>执行流程：请求指定 model 时先按 modelCode 查库；否则读取 deepseek 默认行；
     * 找不到数据库配置时才使用 application.yml 的模型并让 LLM 适配器使用自身兜底连接。
     *
     * @param request 当前消息请求
     * @return 可用于创建或更新 Agent 的运行参数
     */
    public AgentOptions resolve(SendMessageCommand request) {
        boolean hasExplicitModel = request.model() != null && !request.model().isBlank();
        ModelSettingVO requestedSetting = hasExplicitModel ? findByModel(request.model()).orElse(null) : null;
        ModelSettingVO defaultModelSetting = defaultSetting().orElse(null);
        ModelSettingVO setting = requestedSetting != null ? requestedSetting : defaultModelSetting;

        String provider = firstNonBlank(request.provider(),
                setting == null ? null : setting.providerCode(), DEFAULT_PROVIDER);
        String model = firstNonBlank(request.model(),
                setting == null ? null : setting.modelCode(), fallbackModel);

        logModelResolutionGuidance(request.model(), requestedSetting, defaultModelSetting);

        return new AgentOptions(
                provider,
                model,
                request.maxTokens() != null ? request.maxTokens() : 8192,
                setting == null ? null : setting.baseUrl(),
                setting == null ? null : setting.apiKeyRef()
        );
    }

    /**
     * 读取数据库中的全局默认模型配置。
     *
     * @return 已启用的 deepseek 画像配置
     */
    private Optional<ModelSettingVO> defaultSetting() {
        return modelSettingQueryService.findByProfileCode(DEFAULT_MODEL_PROFILE)
                .filter(ModelSettingVO::enabled);
    }

    /**
     * 按请求指定的模型编码查找启用配置。
     *
     * @param modelCode 请求模型编码
     * @return 匹配的第一条启用配置
     */
    private Optional<ModelSettingVO> findByModel(String modelCode) {
        return modelSettingQueryService.queryAll().stream()
                .filter(ModelSettingVO::enabled)
                .filter(setting -> modelCode.equals(setting.modelCode()))
                .findFirst();
    }

    /**
     * 返回第一个非空白字符串。
     */
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    /**
     * 在模型配置缺失或不完整时输出修复建议。
     * <p>流程：请求模型未匹配数据库 → 回退默认模型；默认配置也缺失 → 使用文件兜底；
     * 已命中配置但缺少连接信息 → 提示通过控制台补齐。
     */
    private void logModelResolutionGuidance(
            String requestedModel,
            ModelSettingVO requestedSetting,
            ModelSettingVO defaultSetting
    ) {
        if (requestedModel != null && !requestedModel.isBlank() && requestedSetting == null) {
            if (defaultSetting == null) {
                log.warn("请求模型 '{}' 在数据库中无启用配置，且默认模型也未配置；已使用文件兜底 model={}。"
                                + "请在 Web 控制台模型设置中添加该模型的 provider、baseUrl 和 apiKey。",
                        requestedModel, fallbackModel);
            } else {
                log.warn("请求模型 '{}' 在数据库中无启用配置，已回退到默认模型 '{}={}'。"
                                + "如需使用该请求模型，请在 Web 控制台模型设置中保存其 provider、baseUrl 和 apiKey。",
                        requestedModel, defaultSetting.providerCode(), defaultSetting.modelCode());
            }
        } else if (requestedModel == null || requestedModel.isBlank()) {
            if (defaultSetting == null) {
                log.warn("数据库中不存在启用的默认模型配置 profileCode='{}'；已使用文件兜底 model={}。"
                                + "请在 Web 控制台模型设置中保存默认模型。", DEFAULT_MODEL_PROFILE, fallbackModel);
            }
        }

        ModelSettingVO effectiveSetting = requestedSetting != null ? requestedSetting : defaultSetting;
        if (effectiveSetting == null) {
            return;
        }
        if (isBlank(effectiveSetting.baseUrl())) {
            log.warn("模型 '{}={}' 的 baseUrl 为空，将依赖 LLM 适配器内置兜底地址。"
                            + "请在 Web 控制台模型设置中补齐 baseUrl。",
                    effectiveSetting.providerCode(), effectiveSetting.modelCode());
        }
        if (isBlank(effectiveSetting.apiKeyRef())) {
            log.warn("模型 '{}={}' 的 apiKey 为空，将依赖 LLM 适配器内置兜底密钥。"
                            + "请在 Web 控制台模型设置中补齐 apiKey。",
                    effectiveSetting.providerCode(), effectiveSetting.modelCode());
        }
    }

    /**
     * 判断字符串是否为空或空白。
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
