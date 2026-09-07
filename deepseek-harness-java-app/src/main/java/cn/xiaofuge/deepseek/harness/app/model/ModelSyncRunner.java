package cn.xiaofuge.deepseek.harness.app.model;

import cn.xiaofuge.deepseek.harness.domain.runtime.setting.IModelSettingCommandService;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.IModelSettingQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Startup runner that synchronizes the configured default model into H2.
 * Ensures the model settings endpoint reflects application.yml on each boot.
 */
@Component
public class ModelSyncRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ModelSyncRunner.class);

    private final IModelSettingQueryService queryService;
    private final IModelSettingCommandService commandService;
    private final String defaultModel;
    private final String baseUrl;
    private final ApplicationContext applicationContext;

    /**
     * 初始化默认模型同步器。
     * <p>
     * 流程：读取启动配置 → 注入模型设置读写服务 → 供应用启动时执行同步。
     *
     * @param queryService 模型设置查询服务。
     * @param commandService 模型设置命令服务。
     * @param defaultModel 配置的默认模型编码。
     * @param baseUrl 模型提供方基础地址。
     * @param applicationContext Spring 应用上下文，用于记录启动环境。
     */
    public ModelSyncRunner(
            IModelSettingQueryService queryService,
            IModelSettingCommandService commandService,
            @Value("${harness.llm.deepseek.default-model:glm-5.3-flash}") String defaultModel,
            @Value("${harness.llm.deepseek.base-url:http://127.0.0.1:8777/v1}") String baseUrl,
            ApplicationContext applicationContext
    ) {
        this.queryService = queryService;
        this.commandService = commandService;
        this.defaultModel = defaultModel;
        this.baseUrl = baseUrl;
        this.applicationContext = applicationContext;
    }

    @Override
    /**
     * 应用启动后同步默认模型设置。
     * <p>
     * 流程：查询 deepseek profile → 若默认模型未变化则跳过 → 否则保存最新配置；失败仅记录日志，不阻断启动。
     * <p>
     * 示例：配置默认模型为 glm-5.3-flash，首次启动后模型设置接口返回该模型。
     *
     * @param args Spring Boot 启动参数。
     */
    public void run(ApplicationArguments args) {
        try {
            var existing = queryService.findByProfileCode("deepseek");
            if (existing.isPresent() && defaultModel.equals(existing.get().modelCode())) {
                return;
            }
            commandService.save("deepseek", "deepseek", defaultModel, baseUrl, "", true);
            log.info("Model sync: default model '{}' written to H2 (was '{}')",
                    defaultModel, existing.map(s -> s.modelCode()).orElse("none"));
        } catch (Exception e) {
            log.error("Model sync failed: {}", e.getMessage(), e);
        }
    }
}
