package cn.xiaofuge.deepseek.harness.cases.approval;

import cn.xiaofuge.deepseek.harness.api.dto.ResolveRuntimeApprovalRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.ResolveRuntimeApprovalResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.RuntimeApprovalResponseDTO;
import java.util.List;

public interface IRuntimeApprovalCase {

    List<RuntimeApprovalResponseDTO> listPending();

    ResolveRuntimeApprovalResponseDTO resolve(String approvalId, ResolveRuntimeApprovalRequestDTO request);
}
