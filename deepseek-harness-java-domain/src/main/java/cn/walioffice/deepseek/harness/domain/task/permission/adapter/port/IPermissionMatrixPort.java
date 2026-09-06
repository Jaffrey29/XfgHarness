package cn.walioffice.deepseek.harness.domain.task.permission.adapter.port;

import cn.walioffice.deepseek.harness.domain.task.permission.model.valobj.PermissionMatrixVO;

/**
 * 权限矩阵端口。
 * <p>
 * 执行流程：
 * 1. 领域层发起权限矩阵读取；
 * 2. 基础设施层加载当前配置；
 * 3. 返回给权限策略服务进行判定。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IPermissionMatrixPort {

    /**
     * 读取当前权限矩阵。
     * <p>
     * 执行流程：
     * 1. 从配置或持久化介质加载矩阵；
     * 2. 转换为领域值对象；
     * 3. 返回给领域层使用。
     */
    PermissionMatrixVO load();
}
