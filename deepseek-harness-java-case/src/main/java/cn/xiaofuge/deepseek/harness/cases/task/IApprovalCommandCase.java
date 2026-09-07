package cn.xiaofuge.deepseek.harness.cases.task;

import cn.xiaofuge.deepseek.harness.api.dto.ApprovalActionResponseDTO;

public interface IApprovalCommandCase {
    ApprovalActionResponseDTO approve(String sessionId);
}
