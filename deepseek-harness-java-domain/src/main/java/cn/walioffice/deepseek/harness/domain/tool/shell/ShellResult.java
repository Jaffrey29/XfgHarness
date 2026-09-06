package cn.walioffice.deepseek.harness.domain.tool.shell;

/**
 * Result of a shell command execution.
 *
 * @param stdout     standard output
 * @param stderr     standard error
 * @param exitCode   process exit code (0 = success)
 * @param durationMs wall-clock duration in milliseconds
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record ShellResult(
        String stdout,
        String stderr,
        int exitCode,
        long durationMs
) {
    /** Whether the command succeeded (exit code 0). */
    public boolean isSuccess() {
        return exitCode == 0;
    }
}
