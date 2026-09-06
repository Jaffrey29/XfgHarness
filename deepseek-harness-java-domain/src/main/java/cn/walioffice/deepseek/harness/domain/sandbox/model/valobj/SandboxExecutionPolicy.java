package cn.walioffice.deepseek.harness.domain.sandbox.model.valobj;

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

    public static SandboxExecutionPolicy readOnly() {
        return new SandboxExecutionPolicy(SandboxMode.READ_ONLY, Optional.empty());
    }

    public static SandboxExecutionPolicy workspaceWrite(Path root) {
        return new SandboxExecutionPolicy(SandboxMode.WORKSPACE_WRITE, Optional.of(root));
    }

    public static SandboxExecutionPolicy fullAccess() {
        return new SandboxExecutionPolicy(SandboxMode.DANGER_FULL_ACCESS, Optional.empty());
    }
}
