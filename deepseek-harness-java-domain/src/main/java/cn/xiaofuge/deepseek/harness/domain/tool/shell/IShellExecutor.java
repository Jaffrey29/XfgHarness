package cn.xiaofuge.deepseek.harness.domain.tool.shell;

/**
 * Executor for shell commands.
 * <p>
 * Implementations may run locally, in a sandbox, or remotely.
 * Mirrors the TS {@code ShellExecutor} interface family.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IShellExecutor {

    /**
     * Execute a command and block until completion.
     *
     * @param command  the command string to execute
     * @param cwd      working directory (null = inherit current)
     * @param timeoutMs maximum execution time (0 = no limit)
     * @return the shell result
     */
    ShellResult execute(String command, String cwd, long timeoutMs);
}
