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
