package cn.xiaofuge.deepseek.harness.api;

import cn.xiaofuge.deepseek.harness.api.dto.ApprovalActionResponseDTO;
import cn.xiaofuge.deepseek.harness.api.response.Response;

public interface IHarnessApprovalCommandApi {

    Response<ApprovalActionResponseDTO> approve(String sessionId);
}
