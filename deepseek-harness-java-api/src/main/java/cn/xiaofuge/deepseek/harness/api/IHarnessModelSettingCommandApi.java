package cn.xiaofuge.deepseek.harness.api;

import cn.xiaofuge.deepseek.harness.api.dto.ActivateModelSettingRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.ModelSettingResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.SaveModelSettingRequestDTO;
import cn.xiaofuge.deepseek.harness.api.response.Response;

public interface IHarnessModelSettingCommandApi {

    Response<ModelSettingResponseDTO> save(SaveModelSettingRequestDTO request);

    /**
     * 删除模型设置。
     *
     * @param profileCode 画像编码
     * @param modelCode 模型编码（可空；为空删除该 profile 全部配置）
     */
    Response<Void> delete(String profileCode, String modelCode);

    /**
     * 将某 profile 下的指定模型切换为当前生效模型。
     */
    Response<ModelSettingResponseDTO> activate(ActivateModelSettingRequestDTO request);
}
