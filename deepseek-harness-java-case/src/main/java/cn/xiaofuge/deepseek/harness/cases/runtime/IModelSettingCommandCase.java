package cn.xiaofuge.deepseek.harness.cases.runtime;

import cn.xiaofuge.deepseek.harness.api.dto.SaveModelSettingRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.ModelSettingResponseDTO;

public interface IModelSettingCommandCase {
    ModelSettingResponseDTO save(SaveModelSettingRequestDTO command);
    void delete(String profileCode);
}
