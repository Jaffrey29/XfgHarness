package cn.xiaofuge.deepseek.harness.domain.task;

import cn.xiaofuge.deepseek.harness.domain.task.approval.model.valobj.ApprovalActionVO;

/** Executes approval-center actions for the web console. */
public interface IApprovalCommandService {

    ApprovalActionVO approve(String sessionId);
}
