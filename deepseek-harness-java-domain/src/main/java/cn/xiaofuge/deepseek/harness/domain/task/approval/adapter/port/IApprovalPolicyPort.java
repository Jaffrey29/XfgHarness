package cn.xiaofuge.deepseek.harness.domain.task.approval.adapter.port;

import cn.xiaofuge.deepseek.harness.domain.task.approval.model.valobj.ApprovalPolicyVO;

/**
 * 审批策略端口。
 * <p>
 * 执行流程：
 * 1. 由领域服务发起读取；
 * 2. 基础设施层返回当前审批策略；
 * 3. 领域层据此判断是否进入人工审批。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IApprovalPolicyPort {

    /**
     * 读取当前审批策略。
     * <p>
     * 执行流程：
     * 1. 读取持久化配置或远程配置；
     * 2. 转换为领域值对象；
     * 3. 返回给审批领域服务。
     */
    ApprovalPolicyVO load();
}
