package cn.xiaofuge.deepseek.harness.api;

import cn.xiaofuge.deepseek.harness.api.dto.SubmitTaskRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.SubmitTaskResponseDTO;
import cn.xiaofuge.deepseek.harness.api.response.Response;

public interface IHarnessTaskApi {
    Response<SubmitTaskResponseDTO> submitTask(SubmitTaskRequestDTO request);
}
