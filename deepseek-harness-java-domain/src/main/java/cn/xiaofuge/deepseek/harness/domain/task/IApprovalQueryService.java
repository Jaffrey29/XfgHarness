package cn.xiaofuge.deepseek.harness.domain.task;

import cn.xiaofuge.deepseek.harness.domain.task.approval.model.valobj.PendingApprovalVO;
import java.util.List;

/** Reads approval-center projections for the web console. */
public interface IApprovalQueryService {

    List<PendingApprovalVO> listPendingApprovals(int limit);
}
