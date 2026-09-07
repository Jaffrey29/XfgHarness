package cn.xiaofuge.deepseek.harness.cases.runtime;

import cn.xiaofuge.deepseek.harness.api.dto.SaveModelSettingRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.ModelSettingResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.runtime.factory.RuntimePipelineFactory;
import org.springframework.stereotype.Service;

@Service
public class ModelSettingCommandCaseImpl implements IModelSettingCommandCase {
    private final RuntimePipelineFactory runtimePipelineFactory;

    public ModelSettingCommandCaseImpl(RuntimePipelineFactory runtimePipelineFactory) {
        this.runtimePipelineFactory = runtimePipelineFactory;
    }

    @Override
    public ModelSettingResponseDTO save(SaveModelSettingRequestDTO command) {
        return runtimePipelineFactory.savePipeline().execute(command);
    }

    @Override
    public void delete(String profileCode) {
        runtimePipelineFactory.deletePipeline().execute(profileCode);
    }
}
