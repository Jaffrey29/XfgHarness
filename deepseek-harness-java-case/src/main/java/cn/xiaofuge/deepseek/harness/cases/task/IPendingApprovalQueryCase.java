package cn.xiaofuge.deepseek.harness.cases.task;

import cn.xiaofuge.deepseek.harness.api.dto.PendingApprovalResponseDTO;

import java.util.List;

public interface IPendingApprovalQueryCase {
    List<PendingApprovalResponseDTO> listPendingApprovals(int limit);
}
