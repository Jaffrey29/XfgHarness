package cn.walioffice.deepseek.harness.domain.task;

import cn.walioffice.deepseek.harness.domain.task.approval.model.valobj.ApprovalActionVO;

/** Executes approval-center actions for the web console. */
public interface IApprovalCommandService {

    ApprovalActionVO approve(String sessionId);
}
