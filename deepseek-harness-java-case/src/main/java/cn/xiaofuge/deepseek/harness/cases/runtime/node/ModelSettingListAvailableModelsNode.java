package cn.xiaofuge.deepseek.harness.cases.runtime.node;

import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.api.dto.AvailableModelResponseDTO;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.IModelSettingQueryService;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.model.valobj.ModelSettingVO;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ModelSettingListAvailableModelsNode
        extends AbstractStrategyRouter<Void, NoContext, List<AvailableModelResponseDTO>> {

    private final IModelSettingQueryService modelSettingQueryService;

    public ModelSettingListAvailableModelsNode(IModelSettingQueryService modelSettingQueryService) {
        this.modelSettingQueryService = modelSettingQueryService;
    }

    @Override
    protected List<AvailableModelResponseDTO> doApply(Void request, NoContext context) {
        Map<String, AvailableModelResponseDTO> models = new LinkedHashMap<>();
        modelSettingQueryService.queryAll().stream()
                .filter(ModelSettingVO::enabled)
                .filter(setting -> setting.modelCode() != null && !setting.modelCode().isBlank())
                .forEach(setting -> models.putIfAbsent(
                        setting.providerCode() + ":" + setting.modelCode(),
                        new AvailableModelResponseDTO(setting.providerCode(), setting.modelCode(), setting.modelCode())
                ));
        return List.copyOf(models.values());
    }

    @Override
    public StrategyHandler<Void, NoContext, List<AvailableModelResponseDTO>> getNext(Void request, NoContext context) {
        return defaultStrategyHandler;
    }
}
