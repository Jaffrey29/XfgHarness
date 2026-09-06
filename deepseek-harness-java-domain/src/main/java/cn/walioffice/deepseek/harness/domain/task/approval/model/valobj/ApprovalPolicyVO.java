package cn.walioffice.deepseek.harness.domain.task.approval.model.valobj;

import java.util.Set;

/**
 * 审批策略值对象。
 * <p>
 * 执行流程：
 * 1. 基础设施层读取审批配置；
 * 2. 领域层按执行画像判断是否强制审批；
 * 3. 返回给审批决策服务。
 *
 * @param approvalRequiredProfiles 需要人工审批的执行画像编码
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record ApprovalPolicyVO(Set<String> approvalRequiredProfiles) {

    public ApprovalPolicyVO {
        approvalRequiredProfiles = approvalRequiredProfiles == null ? Set.of() : Set.copyOf(approvalRequiredProfiles);
    }
}
