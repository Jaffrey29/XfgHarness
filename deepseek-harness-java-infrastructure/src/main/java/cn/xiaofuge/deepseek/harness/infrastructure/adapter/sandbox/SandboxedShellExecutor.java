package cn.xiaofuge.deepseek.harness.infrastructure.adapter.sandbox;

import cn.xiaofuge.deepseek.harness.domain.sandbox.ISandboxPolicyService;
import cn.xiaofuge.deepseek.harness.domain.sandbox.model.valobj.SandboxExecutionPolicy;
import cn.xiaofuge.deepseek.harness.domain.sandbox.model.valobj.SandboxPolicyRequest;
import cn.xiaofuge.deepseek.harness.domain.tool.shell.IShellExecutor;
import cn.xiaofuge.deepseek.harness.domain.tool.shell.ShellResult;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * 沙箱感知的 shell 执行器包装。
 * <p>
 * 在 {@link LocalShellExecutor} 外面叠加一层 {@link SandboxPolicyService} 的策略判定：
 * <ul>
 *   <li>{@code READ_ONLY} 模式：直接拒绝一切 shell 执行（shell 必然有副作用）；</li>
 *   <li>{@code WORKSPACE_WRITE} 模式：命令里的 cwd 必须落在会话工作区内，
 *       且命令文本不得命中「越界路径」黑名单（如 {@code cd /}、{@code > /etc/...}）；</li>
 *   <li>{@code DANGER_FULL_ACCESS}：完全放行。</li>
 * </ul>
 * 与 {@code DangerousCommandGuard}（文本级危险命令黑名单）是互补关系：
 * 本类管「空间边界」，Guard 管「内容危险」。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class SandboxedShellExecutor implements IShellExecutor {

    /** 在工作区写模式下禁止的命令前缀/模式（命令文本级，作为 cwd 之外的补充防线） */
    private static final List<String> WORKSPACE_ESCAPE_PATTERNS = List.of(
            "cd /", "cd ~", "cd $HOME",
            ">/etc/", ">/boot/", ">/bin/", ">/sbin/",
            "sudo ", "doas ",
            "mount ", "umount ",
            "chroot ", "nsenter "
    );

    private final IShellExecutor delegate;
    private final ISandboxPolicyService sandboxPolicyService;
    private final String fallbackWorkspaceRoot;

    /**
     * 初始化沙箱 Shell 执行器。
     * <p>流程：读取委托执行器和策略服务 → 对每次命令做边界校验。</p>
     */
    public SandboxedShellExecutor(
            IShellExecutor delegate,
            ISandboxPolicyService sandboxPolicyService,
            String fallbackWorkspaceRoot
    ) {
        this.delegate = delegate;
        this.sandboxPolicyService = sandboxPolicyService;
        this.fallbackWorkspaceRoot = fallbackWorkspaceRoot;
    }

    @Override
    public ShellResult execute(String command, String cwd, long timeoutMs) {
        SandboxExecutionPolicy policy = sandboxPolicyService.resolve(
                new SandboxPolicyRequest(
                        Optional.ofNullable(cwd != null ? Path.of(cwd) : null),
                        Optional.empty()
                )
        );

        return switch (policy.mode()) {
            case READ_ONLY -> new ShellResult(
                    "",
                    "[sandbox] shell_execute denied: current sandbox mode is read-only; " +
                            "shell commands may produce side effects and are not allowed",
                    -100, 0);
            case WORKSPACE_WRITE -> executeInWorkspace(command, cwd, timeoutMs, policy);
            case DANGER_FULL_ACCESS -> delegate.execute(command, cwd, timeoutMs);
        };
    }

    /**
     * 在工作区写模式内执行 Shell 命令。
     * <p>流程：规范化 cwd → 校验工作区边界和逃逸命令 → 委托本地执行器。</p>
     */
    private ShellResult executeInWorkspace(String command, String cwd, long timeoutMs,
                                           SandboxExecutionPolicy policy) {
        // 1. cwd 边界：必须落在 workspaceRoot 内
        Path root = policy.workspaceRoot().orElse(Path.of(fallbackWorkspaceRoot));
        Path target = cwd != null && !cwd.isBlank()
                ? Path.of(cwd).toAbsolutePath().normalize()
                : root;
        if (!target.startsWith(root)) {
            return new ShellResult(
                    "",
                    "[sandbox] shell_execute denied: cwd " + target +
                            " is outside the workspace root " + root,
                    -101, 0);
        }

        // 2. 命令文本级越界黑名单（防 cd 逃逸、sudo 提权等）
        String lower = command.toLowerCase(java.util.Locale.ROOT);
        for (String pattern : WORKSPACE_ESCAPE_PATTERNS) {
            if (lower.contains(pattern.toLowerCase())) {
                return new ShellResult(
                        "",
                        "[sandbox] shell_execute denied under workspace-write: " +
                                "command contains forbidden pattern '" + pattern + "'",
                        -102, 0);
            }
        }

        return delegate.execute(command, target.toString(), timeoutMs);
    }
}
