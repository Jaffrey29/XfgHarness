package cn.xiaofuge.deepseek.harness.domain.task.approval.model.valobj;

import cn.xiaofuge.deepseek.harness.domain.shared.model.valobj.HarnessStatusEnumVO;

/**
 * ApprovalDecisionVO 表示ApprovalDecision的领域值对象。
 */
public record ApprovalDecisionVO(
        HarnessStatusEnumVO status,
        boolean approvalRequired,
        String reason
) {
}
