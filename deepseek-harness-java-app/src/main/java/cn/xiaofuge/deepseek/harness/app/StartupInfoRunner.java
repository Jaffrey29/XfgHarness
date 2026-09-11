package cn.xiaofuge.deepseek.harness.app;

import cn.xiaofuge.deepseek.harness.domain.llm.adapter.port.ILlmRuntimePort;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.IModelSettingQueryService;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.model.valobj.ModelSettingVO;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 输出应用启动后的访问入口与模型配置健康状态。
 *
 * <p>执行流程：Spring MVC 就绪 → 监听 ApplicationReadyEvent → 打印控制台地址 →
 * 校验数据库默认模型 → 校验模型 provider 是否已注册适配器。
 *
 * <p>数据示例：数据库存在 profileCode=default、modelCode=glm-5.3-flash、
 * providerCode=deepseek 的启用记录时，日志会展示该模型和已注册的 provider 列表；
 * 如果没有这条记录，则提示用户通过控制台补齐配置。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Component
public class StartupInfoRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupInfoRunner.class);
    private static final String DEFAULT_MODEL_PROFILE = "default";

    private final IModelSettingQueryService modelSettingQueryService;
    private final ILlmRuntimePort llmRuntimePort;
    private final int port;
    private final String fallbackModel;

    /**
     * 创建启动信息检查器。
     *
     * @param modelSettingQueryService 数据库模型设置查询服务
     * @param llmRuntimePort 已完成适配器注册的 LLM 运行时
     * @param port Web 服务端口
     * @param fallbackModel 文件配置兜底模型
     */
    public StartupInfoRunner(
            IModelSettingQueryService modelSettingQueryService,
            ILlmRuntimePort llmRuntimePort,
            @Value("${server.port:8090}") int port,
            @Value("${harness.llm.deepseek.default-model:${LLM_DEFAULT_MODEL:${DEEPSEEK_DEFAULT_MODEL:gpt-5.5}}}") String fallbackModel
    ) {
        this.modelSettingQueryService = modelSettingQueryService;
        this.llmRuntimePort = llmRuntimePort;
        this.port = port;
        this.fallbackModel = fallbackModel;
    }

    /**
     * Web 服务完全就绪后打印访问入口和模型配置引导。
     *
     * @param event Spring 应用就绪事件
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        String consoleUrl = "http://localhost:%d/".formatted(port);
        log.info("Web 控制台已就绪：{}", consoleUrl);

        String providers = llmRuntimePort.listProviders().stream()
                .map(provider -> provider.id())
                .collect(Collectors.joining(", "));
        log.info("已注册模型 Provider：{}", providers.isBlank() ? "<empty>" : providers);

        try {
            checkDefaultModel();
        } catch (Exception exception) {
            log.warn("模型配置状态检查失败：{}。请检查数据库表 harness_model_setting。", exception.getMessage());
        }
    }

    /**
     * 校验数据库默认模型，并输出可执行的配置建议。
     */
    private void checkDefaultModel() {
        ModelSettingVO setting = modelSettingQueryService.findByProfileCode(DEFAULT_MODEL_PROFILE)
                .orElse(null);
        if (setting == null || !setting.enabled()) {
            log.warn("未找到启用的默认模型配置：profileCode={}，当前将按 model={} 走文件兜底。"
                            + "请打开 Web 控制台，在模型设置中保存 provider、baseUrl 和 apiKey。",
                    DEFAULT_MODEL_PROFILE, fallbackModel);
            return;
        }

        if (isBlank(setting.baseUrl())) {
            log.warn("默认模型 '{}' 的 baseUrl 为空，LLM 适配器可能使用内置兜底地址。"
                            + "建议在 Web 控制台模型设置中补齐 baseUrl。", setting.modelCode());
        }
        if (isBlank(setting.apiKeyRef())) {
            log.warn("默认模型 '{}' 的 apiKey 为空，LLM 适配器可能使用文件或环境变量兜底。"
                            + "建议在 Web 控制台模型设置中补齐 apiKey。", setting.modelCode());
        }
        log.info("当前默认模型：provider={}，model={}，baseUrl={}",
                setting.providerCode(), setting.modelCode(), setting.baseUrl());
    }

    /**
     * 判断字符串是否为空或空白。
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
