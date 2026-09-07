package cn.xiaofuge.deepseek.harness.api;

import cn.xiaofuge.deepseek.harness.api.dto.ModelSettingResponseDTO;
import cn.xiaofuge.deepseek.harness.api.response.Response;
import java.util.List;

public interface IHarnessModelSettingQueryApi {

    Response<List<ModelSettingResponseDTO>> queryAll();
}
