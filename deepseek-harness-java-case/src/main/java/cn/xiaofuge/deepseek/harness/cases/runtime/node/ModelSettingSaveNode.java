package cn.xiaofuge.deepseek.harness.cases.runtime.node;

import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.api.dto.ModelSettingResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.SaveModelSettingRequestDTO;
import cn.xiaofuge.deepseek.harness.cases.support.CaseGuard;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.IModelSettingCommandService;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.model.valobj.ModelSettingVO;
import org.springframework.stereotype.Service;

@Service
public class ModelSettingSaveNode
        extends AbstractStrategyRouter<SaveModelSettingRequestDTO, NoContext, ModelSettingResponseDTO> {

    private final IModelSettingCommandService modelSettingCommandService;

    public ModelSettingSaveNode(IModelSettingCommandService modelSettingCommandService) {
        this.modelSettingCommandService = modelSettingCommandService;
    }

    @Override
    protected ModelSettingResponseDTO doApply(SaveModelSettingRequestDTO command, NoContext context) {
        CaseGuard.requireNonNull(command, "model setting command must not be null");
        CaseGuard.requireText(command.profileCode(), "profileCode must not be blank");
        ModelSettingVO setting = modelSettingCommandService.save(
                command.profileCode(), command.providerCode(), command.modelCode(),
                command.baseUrl(), command.apiKeyRef(), command.enabled()
        );
        return toResponse(setting);
    }

    @Override
    public StrategyHandler<SaveModelSettingRequestDTO, NoContext, ModelSettingResponseDTO> getNext(
            SaveModelSettingRequestDTO command, NoContext context) {
        return defaultStrategyHandler;
    }

    private ModelSettingResponseDTO toResponse(ModelSettingVO setting) {
        return new ModelSettingResponseDTO(
                setting.profileCode(), setting.providerCode(), setting.modelCode(),
                setting.baseUrl(), setting.apiKeyRef(), setting.enabled(), setting.updatedAt()
        );
    }
}
