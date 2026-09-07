package cn.xiaofuge.deepseek.harness.infrastructure.adapter.port;

import cn.xiaofuge.deepseek.harness.domain.task.permission.adapter.port.IPermissionMatrixPort;
import cn.xiaofuge.deepseek.harness.domain.task.permission.model.valobj.PermissionMatrixVO;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Adapts configured tool-permission rules into the permission domain.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Component
public class PermissionMatrixPort implements IPermissionMatrixPort {

    private final List<String> approvalRequiredTools;

    /**
     * 处理Permission Matrix Port相关逻辑。
     */
    public PermissionMatrixPort(@Value("${harness.approval.required-tools:shell_execute,fs_write,plugin.run,subprocess.spawn}") List<String> approvalRequiredTools) {
        this.approvalRequiredTools = List.copyOf(approvalRequiredTools);
    }
    /**
     * 加载。
     */

    @Override
    public PermissionMatrixVO load() {
        return new PermissionMatrixVO(Set.copyOf(approvalRequiredTools));
    }
}
