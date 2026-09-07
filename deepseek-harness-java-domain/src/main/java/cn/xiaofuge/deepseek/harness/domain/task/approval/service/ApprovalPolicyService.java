package cn.xiaofuge.deepseek.harness.domain.task.approval.service;

import cn.xiaofuge.deepseek.harness.domain.task.IApprovalPolicyService;
import cn.xiaofuge.deepseek.harness.domain.task.approval.adapter.port.IApprovalPolicyPort;
import cn.xiaofuge.deepseek.harness.domain.task.approval.model.valobj.ApprovalDecisionVO;
import cn.xiaofuge.deepseek.harness.domain.task.approval.model.valobj.ApprovalPolicyVO;
import cn.xiaofuge.deepseek.harness.domain.task.permission.model.valobj.PermissionAssessmentVO;
import cn.xiaofuge.deepseek.harness.domain.shared.model.valobj.HarnessStatusEnumVO;
import org.springframework.stereotype.Service;

/**
 * 审批决策服务。
 * <p>
 * 执行流程：
 * 1. 读取当前审批策略；
 * 2. 先判断执行画像是否强制审批；
 * 3. 再判断工具权限是否触发审批，最终返回决策结果。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class ApprovalPolicyService implements IApprovalPolicyService {

    private final IApprovalPolicyPort approvalPolicyPort;

    /**
     * 处理Approval Policy Service相关逻辑。
     */
    public ApprovalPolicyService(IApprovalPolicyPort approvalPolicyPort) {
        this.approvalPolicyPort = approvalPolicyPort;
    }

    /**
     * 生成审批决策。
     * <p>
     * 执行流程：
     * 1. 读取审批策略；
     * 2. 按画像和工具权限依次判断；
     * 3. 返回最终决策对象。
     */
    @Override
    public ApprovalDecisionVO decide(String profileCode, PermissionAssessmentVO permissionAssessment) {
        ApprovalPolicyVO approvalPolicy = approvalPolicyPort.load();
        if (approvalPolicy.approvalRequiredProfiles().contains(profileCode)) {
            return new ApprovalDecisionVO(
                    HarnessStatusEnumVO.PENDING_APPROVAL,
                    true,
                    "执行画像需要人工审批：" + profileCode
            );
        }
        if (permissionAssessment.approvalRequired()) {
            return new ApprovalDecisionVO(
                    HarnessStatusEnumVO.PENDING_APPROVAL,
                    true,
                    "工具需要人工审批：" + String.join(",", permissionAssessment.approvalRequiredTools())
            );
        }
        return new ApprovalDecisionVO(HarnessStatusEnumVO.QUEUED, false, "任务自动通过");
    }
}
