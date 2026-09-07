package cn.xiaofuge.deepseek.harness.domain.sandbox.model.valobj;

import java.nio.file.Path;
import java.util.Optional;

/**
 * SandboxPolicyRequest 表示SandboxPolicyRequest请求。
 */
public record SandboxPolicyRequest(Optional<Path> sessionCwd, Optional<SandboxMode> modeOverride) {
}
