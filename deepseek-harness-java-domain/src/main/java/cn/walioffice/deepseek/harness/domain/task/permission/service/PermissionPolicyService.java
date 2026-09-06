package cn.walioffice.deepseek.harness.domain.task.permission.service;

import cn.walioffice.deepseek.harness.domain.task.IPermissionPolicyService;
import cn.walioffice.deepseek.harness.domain.task.permission.adapter.port.IPermissionMatrixPort;
import cn.walioffice.deepseek.harness.domain.task.permission.model.valobj.PermissionAssessmentVO;
import cn.walioffice.deepseek.harness.domain.task.permission.model.valobj.PermissionMatrixVO;
import java.util.List;

/**
 * 权限评估服务。
 * <p>
 * 执行流程：
 * 1. 读取权限矩阵；
 * 2. 将启用工具拆分为需审批和可自动通过两类；
 * 3. 返回权限评估结果。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class PermissionPolicyService implements IPermissionPolicyService {

    private final IPermissionMatrixPort permissionMatrixPort;

    public PermissionPolicyService(IPermissionMatrixPort permissionMatrixPort) {
        this.permissionMatrixPort = permissionMatrixPort;
    }

    /**
     * 评估启用工具权限。
     * <p>
     * 执行流程：
     * 1. 读取权限矩阵；
     * 2. 过滤出需要审批的工具；
     * 3. 组装评估结果返回。
     */
    @Override
    public PermissionAssessmentVO assess(List<String> enabledTools) {
        PermissionMatrixVO permissionMatrix = permissionMatrixPort.load();
        List<String> gatedTools = enabledTools.stream()
                .filter(permissionMatrix.approvalRequiredTools()::contains)
                .toList();
        List<String> autoApprovedTools = enabledTools.stream()
                .filter(toolCode -> !permissionMatrix.approvalRequiredTools().contains(toolCode))
                .toList();
        return new PermissionAssessmentVO(enabledTools, gatedTools, autoApprovedTools, !gatedTools.isEmpty());
    }
}
