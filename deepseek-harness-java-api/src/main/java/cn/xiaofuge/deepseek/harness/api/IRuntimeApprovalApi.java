package cn.xiaofuge.deepseek.harness.api;

import cn.xiaofuge.deepseek.harness.api.dto.ResolveRuntimeApprovalRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.ResolveRuntimeApprovalResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.RuntimeApprovalResponseDTO;
import cn.xiaofuge.deepseek.harness.api.response.Response;
import java.util.List;

public interface IRuntimeApprovalApi {

    Response<List<RuntimeApprovalResponseDTO>> listPending();

    Response<ResolveRuntimeApprovalResponseDTO> resolve(
            String approvalId,
            ResolveRuntimeApprovalRequestDTO request
    );
}
