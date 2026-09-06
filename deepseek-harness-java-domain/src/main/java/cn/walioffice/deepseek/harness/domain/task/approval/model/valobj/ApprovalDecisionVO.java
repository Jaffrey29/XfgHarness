package cn.walioffice.deepseek.harness.domain.task.approval.model.valobj;

import cn.walioffice.deepseek.harness.domain.shared.model.valobj.HarnessStatusEnumVO;

/**
 * Describes the approval outcome for one task submission.
 *
 * @param status lifecycle status that should be persisted for the task/session
 * @param approvalRequired whether the task must wait for human approval
 * @param reason short explanation for the decision
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record ApprovalDecisionVO(
        HarnessStatusEnumVO status,
        boolean approvalRequired,
        String reason
) {
}
