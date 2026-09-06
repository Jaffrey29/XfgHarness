package cn.walioffice.deepseek.harness.domain.sandbox.service;

import cn.walioffice.deepseek.harness.domain.sandbox.ISandboxPolicyService;
import cn.walioffice.deepseek.harness.domain.sandbox.model.valobj.SandboxExecutionPolicy;
import cn.walioffice.deepseek.harness.domain.sandbox.model.valobj.SandboxMode;
import cn.walioffice.deepseek.harness.domain.sandbox.model.valobj.SandboxPolicyRequest;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Owns the deployment default mode, fallback workspace root, and request-time
 * policy resolution. Tool layers call {@link #resolve} for each execution so a
 * session's mode and immutable cwd travel together to every enforcing capability.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class SandboxPolicyService implements ISandboxPolicyService {

    private final SandboxMode defaultMode;
    private final Path fallbackWorkspaceRoot;

    public SandboxPolicyService(SandboxMode defaultMode, Path fallbackWorkspaceRoot) {
        this.defaultMode = defaultMode;
        this.fallbackWorkspaceRoot = fallbackWorkspaceRoot.toAbsolutePath().normalize();
    }

    @Override
    public SandboxExecutionPolicy resolve(SandboxPolicyRequest request) {
        SandboxMode mode = request.modeOverride().orElse(defaultMode);
        Path root = request.sessionCwd().orElse(fallbackWorkspaceRoot);
        return switch (mode) {
            case READ_ONLY -> SandboxExecutionPolicy.readOnly();
            case WORKSPACE_WRITE -> SandboxExecutionPolicy.workspaceWrite(root);
            case DANGER_FULL_ACCESS -> SandboxExecutionPolicy.fullAccess();
        };
    }

    public String renderPolicyContext(SandboxExecutionPolicy policy) {
        return switch (policy.mode()) {
            case READ_ONLY -> "Current DSH file policy: read-only. Any available operation enforced by the DSH file sandbox cannot modify files in the standing mode.";
            case WORKSPACE_WRITE -> "Current DSH file policy: workspace-write. Any available operation enforced by the DSH file sandbox may modify files under the session workspace: " + policy.workspaceRoot().map(Path::toString).orElse("") + ".";
            case DANGER_FULL_ACCESS -> "Current DSH file policy: danger-full-access. The DSH file sandbox does not restrict file modifications by available operations.";
        };
    }
}
