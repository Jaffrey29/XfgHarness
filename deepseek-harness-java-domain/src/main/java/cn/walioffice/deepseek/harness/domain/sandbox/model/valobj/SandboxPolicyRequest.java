package cn.walioffice.deepseek.harness.domain.sandbox.model.valobj;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Inputs that select the sandbox policy for one capability call.
 *
 * @param sessionCwd   session's immutable cwd (empty for agentless calls)
 * @param modeOverride explicit approved mode override (outranks default)
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record SandboxPolicyRequest(Optional<Path> sessionCwd, Optional<SandboxMode> modeOverride) {
}
