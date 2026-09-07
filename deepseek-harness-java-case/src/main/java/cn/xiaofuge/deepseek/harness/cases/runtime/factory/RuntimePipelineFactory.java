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

/**
 * 模型设置 Pipeline 工厂。
 * <p>流程：按写/读目标组装四个独立 Pipeline，分别路由到保存、删除、配置列表和可用模型节点。</p>
 * <p>数据示例：保存传入 {@code profileCode=default} 返回配置 DTO；
 * 删除传入 {@code profileCode=default} 无返回；另两个查询无入参并返回列表。</p>
 */
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

    /**
     * 构建保存模型设置的 Pipeline。
     */
    public CasePipeline<SaveModelSettingRequestDTO, NoContext, ModelSettingResponseDTO> savePipeline() {
        return CasePipeline.of("Save model setting", modelSettingSaveNode, () -> NoContext.INSTANCE);
    }

    /**
     * 构建删除模型设置的 Pipeline。
     */
    public CasePipeline<String, NoContext, Void> deletePipeline() {
        return CasePipeline.of("Delete model setting", modelSettingDeleteNode, () -> NoContext.INSTANCE);
    }

    /**
     * 构建查询模型设置列表的 Pipeline。
     */
    public CasePipeline<Void, NoContext, List<ModelSettingResponseDTO>> listSettingsPipeline() {
        return CasePipeline.of("List model settings", modelSettingListSettingsNode, () -> NoContext.INSTANCE);
    }

    /**
     * 构建查询可用模型列表的 Pipeline。
     */
    public CasePipeline<Void, NoContext, List<AvailableModelResponseDTO>> listAvailableModelsPipeline() {
        return CasePipeline.of("List available models", modelSettingListAvailableModelsNode, () -> NoContext.INSTANCE);
    }

    /**
     * 构建默认执行 Pipeline，绑定首节点和上下文。
     */
    @Override
    public CasePipeline<SaveModelSettingRequestDTO, NoContext, ModelSettingResponseDTO> pipeline() {
        return savePipeline();
    }
}
