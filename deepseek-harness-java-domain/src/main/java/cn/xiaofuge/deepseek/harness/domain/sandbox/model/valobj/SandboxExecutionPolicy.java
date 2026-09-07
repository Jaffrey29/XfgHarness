package cn.xiaofuge.deepseek.harness.domain.sandbox.model.valobj;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Resolved sandbox policy for one capability call.
 *
 * @param mode          file-effect mode
 * @param workspaceRoot absolute workspace root for workspace-write mode
 *
 * <p>调用流程：应用领域规则 → 更新聚合或值对象 → 发布事件/返回结果。</p>
 * <p>示例：领域对象只暴露业务语言方法，不直接依赖基础设施。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public record SandboxExecutionPolicy(SandboxMode mode, Optional<Path> workspaceRoot) {

    /**
     * 读取 Only。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @return 方法执行结果。
     */
    public static SandboxExecutionPolicy readOnly() {
        return new SandboxExecutionPolicy(SandboxMode.READ_ONLY, Optional.empty());
    }

    /**
     * 处理工作区 Write。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param root 根路径。
     * @return 方法执行结果。
     */
    public static SandboxExecutionPolicy workspaceWrite(Path root) {
        return new SandboxExecutionPolicy(SandboxMode.WORKSPACE_WRITE, Optional.of(root));
    }

    /**
     * 处理full Access。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @return 方法执行结果。
     */
    public static SandboxExecutionPolicy fullAccess() {
        return new SandboxExecutionPolicy(SandboxMode.DANGER_FULL_ACCESS, Optional.empty());
    }
}
