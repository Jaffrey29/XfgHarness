package cn.xiaofuge.deepseek.harness.cases.runtime;

import cn.xiaofuge.deepseek.harness.api.dto.AvailableModelResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.ModelSettingResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.runtime.factory.RuntimePipelineFactory;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ModelSettingQueryCaseImpl implements IModelSettingQueryCase {
    private final RuntimePipelineFactory runtimePipelineFactory;

    public ModelSettingQueryCaseImpl(RuntimePipelineFactory runtimePipelineFactory) {
        this.runtimePipelineFactory = runtimePipelineFactory;
    }

    @Override
    public List<ModelSettingResponseDTO> listSettings() {
        return runtimePipelineFactory.listSettingsPipeline().execute(null);
    }

    @Override
    public List<AvailableModelResponseDTO> listAvailableModels() {
        return runtimePipelineFactory.listAvailableModelsPipeline().execute(null);
    }
}
