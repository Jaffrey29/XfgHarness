package cn.xiaofuge.deepseek.harness.domain.sandbox.service;

import cn.xiaofuge.deepseek.harness.domain.sandbox.ISandboxPolicyService;
import cn.xiaofuge.deepseek.harness.domain.sandbox.model.valobj.SandboxExecutionPolicy;
import cn.xiaofuge.deepseek.harness.domain.sandbox.model.valobj.SandboxMode;
import cn.xiaofuge.deepseek.harness.domain.sandbox.model.valobj.SandboxPolicyRequest;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * SandboxPolicyService 封装SandboxPolicy的领域规则与流程。
 */
@Service
public class SandboxPolicyService implements ISandboxPolicyService {

    private final SandboxMode defaultMode;
    private final Path fallbackWorkspaceRoot;

    /**
     * 初始化沙箱策略服务。
     * <p>流程：读取执行上下文 → 解析读写模式 → 返回允许的操作边界。</p>
     */
    public SandboxPolicyService(
            @Value("${harness.sandbox.default-mode:READ_ONLY}") String defaultMode,
            @Value("${harness.agent.default-cwd:#{systemProperties['user.dir']}}") String fallbackWorkspaceRoot
    ) {
        this.defaultMode = SandboxMode.valueOf(defaultMode.trim().toUpperCase().replace('-', '_'));
        this.fallbackWorkspaceRoot = Path.of(fallbackWorkspaceRoot).toAbsolutePath().normalize();
    }

    @Override
    public SandboxExecutionPolicy resolve(SandboxPolicyRequest request) {
        SandboxMode mode = request.modeOverride().orElse(defaultMode);
        Path root = request.sessionCwd()
                .map(Path::toAbsolutePath)
                .map(Path::normalize)
                .orElse(fallbackWorkspaceRoot);
        return switch (mode) {
            case READ_ONLY -> SandboxExecutionPolicy.readOnly();
            case WORKSPACE_WRITE -> SandboxExecutionPolicy.workspaceWrite(root);
            case DANGER_FULL_ACCESS -> SandboxExecutionPolicy.fullAccess();
        };
    }

    @Override
    public SandboxExecutionPolicy resolveWithExtraRoots(SandboxPolicyRequest request, java.util.List<Path> extraRoots) {
        SandboxMode mode = request.modeOverride().orElse(defaultMode);
        if (mode != SandboxMode.WORKSPACE_WRITE || extraRoots == null || extraRoots.isEmpty()) {
            return resolve(request);
        }
        java.util.List<Path> roots = new java.util.ArrayList<>();
        roots.add(request.sessionCwd()
                .map(Path::toAbsolutePath)
                .map(Path::normalize)
                .orElse(fallbackWorkspaceRoot));
        for (Path extra : extraRoots) {
            if (extra == null) continue;
            Path normalized = extra.toAbsolutePath().normalize();
            if (roots.stream().noneMatch(normalized::equals)) {
                roots.add(normalized);
            }
        }
        return SandboxExecutionPolicy.workspaceWrite(roots);
    }

    /**
     * 处理render 策略 上下文。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param policy 策略对象。
     * @return 方法执行结果。
     */
    public String renderPolicyContext(SandboxExecutionPolicy policy) {
        return switch (policy.mode()) {
            case READ_ONLY -> "Current DSH file policy: read-only. Any available operation enforced by the DSH file sandbox cannot modify files in the standing mode.";
            case WORKSPACE_WRITE -> {
                java.util.List<Path> roots = policy.workspaceRoots() != null && !policy.workspaceRoots().isEmpty()
                        ? policy.workspaceRoots()
                        : policy.workspaceRoot().map(java.util.List::of).orElse(java.util.List.of());
                String joined = roots.stream().map(Path::toString).collect(java.util.stream.Collectors.joining(", "));
                yield "Current DSH file policy: workspace-write. Any available operation enforced by the DSH file sandbox may modify files under these writable roots: " + joined + ".";
            }
            case DANGER_FULL_ACCESS -> "Current DSH file policy: danger-full-access. The DSH file sandbox does not restrict file modifications by available operations.";
        };
    }
}
