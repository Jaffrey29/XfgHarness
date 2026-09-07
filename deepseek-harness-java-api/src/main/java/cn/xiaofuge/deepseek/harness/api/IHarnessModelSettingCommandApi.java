package cn.xiaofuge.deepseek.harness.api;

import cn.xiaofuge.deepseek.harness.api.dto.ModelSettingResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.SaveModelSettingRequestDTO;
import cn.xiaofuge.deepseek.harness.api.response.Response;

public interface IHarnessModelSettingCommandApi {

    Response<ModelSettingResponseDTO> save(SaveModelSettingRequestDTO request);

    Response<Void> delete(String profileCode);
}
