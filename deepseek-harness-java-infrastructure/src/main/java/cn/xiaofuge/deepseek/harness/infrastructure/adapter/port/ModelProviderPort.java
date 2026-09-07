package cn.xiaofuge.deepseek.harness.infrastructure.adapter.port;

import cn.xiaofuge.deepseek.harness.domain.runtime.model.adapter.port.IModelProviderPort;
import cn.xiaofuge.deepseek.harness.domain.runtime.model.model.valobj.ModelProviderProfileVO;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.IModelSettingQueryService;
import org.springframework.stereotype.Component;

/**
 * 模型提供方端口适配器。
 * <p>
 * 从持久化模型设置中解析 profileCode 对应的 provider 和 model。
 * <p>调用流程：领域端口 → 基础设施适配器 → 外部系统。</p>
 * <p>示例：适配器只实现端口契约，业务规则保留在 domain。</p>
 */
@Component
public class ModelProviderPort implements IModelProviderPort {

    private final IModelSettingQueryService modelSettingService;

    public ModelProviderPort(IModelSettingQueryService modelSettingService) {
        this.modelSettingService = modelSettingService;
    }

    @Override
    public ModelProviderProfileVO resolve(String profileCode) {
        return modelSettingService.findByProfileCode(profileCode)
                .filter(s -> s.enabled())
                .map(s -> new ModelProviderProfileVO(s.providerCode(), s.modelCode()))
                .orElse(new ModelProviderProfileVO("deepseek", "glm-5.3-flash"));
    }
}
