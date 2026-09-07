package cn.xiaofuge.deepseek.harness.infrastructure.adapter.port;

import cn.xiaofuge.deepseek.harness.domain.task.approval.adapter.port.IApprovalPolicyPort;
import cn.xiaofuge.deepseek.harness.domain.task.approval.model.valobj.ApprovalPolicyVO;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Adapts configured approval-profile rules into the approval domain.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Component
public class ApprovalPolicyPort implements IApprovalPolicyPort {

    private final List<String> approvalRequiredProfiles;

    /**
     * 处理Approval Policy Port相关逻辑。
     */
    public ApprovalPolicyPort(@Value("${harness.approval.required-profiles:web}") List<String> approvalRequiredProfiles) {
        this.approvalRequiredProfiles = List.copyOf(approvalRequiredProfiles);
    }
    /**
     * 加载。
     */

    @Override
    public ApprovalPolicyVO load() {
        return new ApprovalPolicyVO(Set.copyOf(approvalRequiredProfiles));
    }
}
