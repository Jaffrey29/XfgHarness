package cn.xiaofuge.deepseek.harness.infrastructure.adapter.port;

import cn.xiaofuge.deepseek.harness.domain.runtime.model.adapter.port.IModelProviderPort;
import cn.xiaofuge.deepseek.harness.domain.runtime.model.model.valobj.ModelProviderProfileVO;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.IModelSettingQueryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 模型提供方端口适配器。
 * <p>
 * 从持久化模型渠道中解析当前激活的 provider 和 model。
 * <p>调用流程：领域端口 → 基础设施适配器 → 外部系统。</p>
 * <p>示例：适配器只实现端口契约，业务规则保留在 domain。</p>
 */
@Component
public class ModelProviderPort implements IModelProviderPort {

    private final IModelSettingQueryService modelSettingService;
    private final String fallbackChannel;
    private final String fallbackModel;

    public ModelProviderPort(IModelSettingQueryService modelSettingService,
                             @Value("${harness.llm.deepseek.default-channel:${LLM_DEFAULT_CHANNEL:default}}") String fallbackChannel,
                             @Value("${harness.llm.deepseek.default-model:${LLM_DEFAULT_MODEL:${DEEPSEEK_DEFAULT_MODEL:glm-5.3-flash}}}") String fallbackModel) {
        this.modelSettingService = modelSettingService;
        this.fallbackChannel = fallbackChannel;
        this.fallbackModel = fallbackModel;
    }

    @Override
    public ModelProviderProfileVO resolve(String channelCode) {
        var setting = hasText(channelCode)
                ? modelSettingService.findByChannelCode(channelCode)
                : modelSettingService.findActive();
        return setting
                .filter(s -> s.enabled())
                .map(s -> new ModelProviderProfileVO(
                        s.channelCode(), s.providerCode(), s.modelCode()))
                .orElse(new ModelProviderProfileVO(fallbackChannel, "deepseek", fallbackModel));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
