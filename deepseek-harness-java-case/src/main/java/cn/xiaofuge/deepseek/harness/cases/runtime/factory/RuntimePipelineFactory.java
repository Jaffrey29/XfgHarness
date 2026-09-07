package cn.xiaofuge.deepseek.harness.cases.runtime.factory;

import cn.xiaofuge.deepseek.harness.cases.orchestration.CasePipeline;
import cn.xiaofuge.deepseek.harness.cases.orchestration.CasePipelineFactory;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.api.dto.AvailableModelResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.ModelSettingResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.SaveModelSettingRequestDTO;
import cn.xiaofuge.deepseek.harness.cases.runtime.node.ModelSettingDeleteNode;
import cn.xiaofuge.deepseek.harness.cases.runtime.node.ModelSettingListAvailableModelsNode;
import cn.xiaofuge.deepseek.harness.cases.runtime.node.ModelSettingListSettingsNode;
import cn.xiaofuge.deepseek.harness.cases.runtime.node.ModelSettingSaveNode;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RuntimePipelineFactory
        implements CasePipelineFactory<SaveModelSettingRequestDTO, NoContext, ModelSettingResponseDTO> {

    private final ModelSettingSaveNode modelSettingSaveNode;
    private final ModelSettingDeleteNode modelSettingDeleteNode;
    private final ModelSettingListSettingsNode modelSettingListSettingsNode;
    private final ModelSettingListAvailableModelsNode modelSettingListAvailableModelsNode;

    public RuntimePipelineFactory(
            ModelSettingSaveNode modelSettingSaveNode,
            ModelSettingDeleteNode modelSettingDeleteNode,
            ModelSettingListSettingsNode modelSettingListSettingsNode,
            ModelSettingListAvailableModelsNode modelSettingListAvailableModelsNode
    ) {
        this.modelSettingSaveNode = modelSettingSaveNode;
        this.modelSettingDeleteNode = modelSettingDeleteNode;
        this.modelSettingListSettingsNode = modelSettingListSettingsNode;
        this.modelSettingListAvailableModelsNode = modelSettingListAvailableModelsNode;
    }

    public CasePipeline<SaveModelSettingRequestDTO, NoContext, ModelSettingResponseDTO> savePipeline() {
        return CasePipeline.of("Save model setting", modelSettingSaveNode, () -> NoContext.INSTANCE);
    }

    public CasePipeline<String, NoContext, Void> deletePipeline() {
        return CasePipeline.of("Delete model setting", modelSettingDeleteNode, () -> NoContext.INSTANCE);
    }

    public CasePipeline<Void, NoContext, List<ModelSettingResponseDTO>> listSettingsPipeline() {
        return CasePipeline.of("List model settings", modelSettingListSettingsNode, () -> NoContext.INSTANCE);
    }

    public CasePipeline<Void, NoContext, List<AvailableModelResponseDTO>> listAvailableModelsPipeline() {
        return CasePipeline.of("List available models", modelSettingListAvailableModelsNode, () -> NoContext.INSTANCE);
    }

    @Override
    public CasePipeline<SaveModelSettingRequestDTO, NoContext, ModelSettingResponseDTO> pipeline() {
        return savePipeline();
    }
}
