package cn.walioffice.deepseek.harness.domain.task.permission.model.valobj;

import java.util.Set;

/**
 * 权限矩阵值对象。
 * <p>
 * 执行流程：
 * 1. 基础设施层读取权限配置；
 * 2. 领域层根据矩阵判断工具是否需要审批；
 * 3. 返回给权限策略服务。
 *
 * @param approvalRequiredTools 需要人工审批的工具编码
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record PermissionMatrixVO(Set<String> approvalRequiredTools) {

    public PermissionMatrixVO {
        approvalRequiredTools = approvalRequiredTools == null ? Set.of() : Set.copyOf(approvalRequiredTools);
    }
}
