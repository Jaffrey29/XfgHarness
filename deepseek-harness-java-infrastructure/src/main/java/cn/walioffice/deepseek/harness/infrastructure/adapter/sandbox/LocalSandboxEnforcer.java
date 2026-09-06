package cn.walioffice.deepseek.harness.infrastructure.adapter.sandbox;

import cn.walioffice.deepseek.harness.domain.sandbox.model.valobj.SandboxExecutionPolicy;
import cn.walioffice.deepseek.harness.domain.sandbox.model.valobj.SandboxMode;
import java.nio.file.Path;

/**
 * Local filesystem sandbox enforcer. Checks whether a path operation is allowed
 * under the resolved policy.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class LocalSandboxEnforcer {

    /**
     * Check whether a write to the given path is allowed under the policy.
     *
     * @param policy resolved execution policy
     * @param path   target path
     * @return true if the write is allowed
     */
    public boolean canWrite(SandboxExecutionPolicy policy, Path path) {
        return switch (policy.mode()) {
            case DANGER_FULL_ACCESS -> true;
            case WORKSPACE_WRITE -> {
                Path root = policy.workspaceRoot().orElse(null);
                if (root == null) yield false;
                Path normalized = path.toAbsolutePath().normalize();
                Path normalizedRoot = root.toAbsolutePath().normalize();
                yield normalized.startsWith(normalizedRoot);
            }
            case READ_ONLY -> false;
        };
    }

    /**
     * Check whether a read from the given path is allowed. Reads are always
     * allowed under any mode (the sandbox restricts writes, not reads).
     */
    public boolean canRead(SandboxExecutionPolicy policy, Path path) {
        return true;
    }

    public SandboxMode modeOf(SandboxExecutionPolicy policy) {
        return policy.mode();
    }
}
