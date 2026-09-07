package cn.xiaofuge.deepseek.harness.domain.task;

import cn.xiaofuge.deepseek.harness.domain.task.approval.model.valobj.ApprovalActionVO;

/**
 * IApprovalCommandService 封装IApprovalCommand命令的领域规则与流程。
 */
public interface IApprovalCommandService {

    ApprovalActionVO approve(String sessionId);
}
