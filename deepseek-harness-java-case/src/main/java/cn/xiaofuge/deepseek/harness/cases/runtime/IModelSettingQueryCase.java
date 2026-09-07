package cn.xiaofuge.deepseek.harness.cases.runtime;

import cn.xiaofuge.deepseek.harness.api.dto.AvailableModelResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.ModelSettingResponseDTO;
import java.util.List;

public interface IModelSettingQueryCase {
    List<ModelSettingResponseDTO> listSettings();
    List<AvailableModelResponseDTO> listAvailableModels();
}
