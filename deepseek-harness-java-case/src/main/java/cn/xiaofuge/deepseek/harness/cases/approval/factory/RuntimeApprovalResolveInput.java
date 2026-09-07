package cn.xiaofuge.deepseek.harness.cases.approval.factory;

import cn.xiaofuge.deepseek.harness.domain.agent.model.valobj.RuntimeApprovalDecisionVO;

public record RuntimeApprovalResolveInput(String approvalId, RuntimeApprovalDecisionVO decision) {
}
