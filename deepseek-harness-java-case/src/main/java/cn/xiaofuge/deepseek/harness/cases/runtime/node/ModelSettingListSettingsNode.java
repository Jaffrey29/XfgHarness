package cn.xiaofuge.deepseek.harness.cases.runtime.node;

import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.api.dto.ModelSettingResponseDTO;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.IModelSettingQueryService;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.model.valobj.ModelSettingVO;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ModelSettingListSettingsNode
        extends AbstractStrategyRouter<Void, NoContext, List<ModelSettingResponseDTO>> {

    private final IModelSettingQueryService modelSettingQueryService;

    public ModelSettingListSettingsNode(IModelSettingQueryService modelSettingQueryService) {
        this.modelSettingQueryService = modelSettingQueryService;
    }

    @Override
    protected List<ModelSettingResponseDTO> doApply(Void request, NoContext context) {
        return modelSettingQueryService.queryAll().stream().map(this::toResponse).toList();
    }

    @Override
    public StrategyHandler<Void, NoContext, List<ModelSettingResponseDTO>> getNext(Void request, NoContext context) {
        return defaultStrategyHandler;
    }

    private ModelSettingResponseDTO toResponse(ModelSettingVO setting) {
        return new ModelSettingResponseDTO(
                setting.profileCode(), setting.providerCode(), setting.modelCode(),
                setting.baseUrl(), setting.apiKeyRef(), setting.enabled(), setting.updatedAt()
        );
    }
}
