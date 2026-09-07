package cn.xiaofuge.deepseek.harness.domain.sandbox.model.valobj;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Resolved sandbox policy for one capability call.
 *
 * @param mode          file-effect mode
 * @param workspaceRoot absolute workspace root for workspace-write mode
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record SandboxExecutionPolicy(SandboxMode mode, Optional<Path> workspaceRoot) {

    /**
     * 处理read Only相关逻辑。
     */
    public static SandboxExecutionPolicy readOnly() {
        return new SandboxExecutionPolicy(SandboxMode.READ_ONLY, Optional.empty());
    }

    /**
     * 处理workspace Write相关逻辑。
     */
    public static SandboxExecutionPolicy workspaceWrite(Path root) {
        return new SandboxExecutionPolicy(SandboxMode.WORKSPACE_WRITE, Optional.of(root));
    }

    /**
     * 处理full Access相关逻辑。
     */
    public static SandboxExecutionPolicy fullAccess() {
        return new SandboxExecutionPolicy(SandboxMode.DANGER_FULL_ACCESS, Optional.empty());
    }
}
