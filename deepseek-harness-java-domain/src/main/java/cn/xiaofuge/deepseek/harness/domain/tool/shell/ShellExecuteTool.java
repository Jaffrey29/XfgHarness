package cn.xiaofuge.deepseek.harness.domain.tool.shell;

import cn.xiaofuge.deepseek.harness.domain.model.valobj.ContentBlock;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.TextBlock;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolDefinition;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolExecutionResult;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolRunContext;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * The {@code shell_execute} tool.
 * <p>
 * Executes a shell command and returns stdout/stderr/exitCode to the model.
 * Requires approval from the permission matrix before execution.
 * Mirrors TS {@code tool-bash} / {@code tool-pwsh}.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class ShellExecuteTool implements ToolDefinition {

    private static final String NAME = "shell_execute";

    private static final String DESCRIPTION =
            "Execute a shell command on the host system. " +
            "Use this for running scripts, inspecting files, building projects, " +
            "git operations, and other command-line tasks. " +
            "The command runs in the agent's working directory unless 'cwd' is specified. " +
            "Output is truncated to 50000 characters.";

    private static final int MAX_OUTPUT = 50000;
    private static final long DEFAULT_TIMEOUT_MS = 120_000; // 2 minutes

    private final IShellExecutor executor;
    private final String defaultCwd;
    private final DangerousCommandGuard guard;

    /**
     * 处理Shell Execute Tool相关逻辑。
     */
    public ShellExecuteTool(IShellExecutor executor, String defaultCwd) {
        this(executor, defaultCwd, new DangerousCommandGuard());
    }

    /**
     * 处理Shell Execute Tool相关逻辑。
     */
    public ShellExecuteTool(IShellExecutor executor, String defaultCwd, DangerousCommandGuard guard) {
        this.executor = executor;
        this.defaultCwd = defaultCwd;
        this.guard = guard == null ? new DangerousCommandGuard() : guard;
    }

    @Override public String name() { return NAME; }
    @Override public String description() { return DESCRIPTION; }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "command", Map.of(
                                "type", "string",
                                "description", "The shell command to execute."
                        ),
                        "cwd", Map.of(
                                "type", "string",
                                "description", "Optional working directory. Defaults to the agent's cwd."
                        ),
                        "timeout_ms", Map.of(
                                "type", "integer",
                                "description", "Optional timeout in milliseconds. Default 120000 (2 min)."
                        )
                ),
                "required", List.of("command")
        );
    }

    @Override
    public Map<String, Object> outputSchema() {
        return Map.of(
                "type", "object",
                "additionalProperties", true,
                "properties", Map.of(
                        "stdout", Map.of("type", "string"),
                        "stderr", Map.of("type", "string"),
                        "exit_code", Map.of("type", "integer"),
                        "duration_ms", Map.of("type", "integer")
                )
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<ToolExecutionResult> execute(Object args, ToolRunContext ctx) {
        try {
            Map<String, Object> argMap = (Map<String, Object>) args;
            String command = String.valueOf(argMap.get("command"));
            if (command == null || command.isBlank()) {
                return CompletableFuture.completedFuture(
                        ToolExecutionResult.fail("command parameter is required", "MISSING_PARAM"));
            }

            // 危险命令静态拦截（审批之外的第二道防线）
            String blockedReason = guard.check(command);
            if (blockedReason != null) {
                return CompletableFuture.completedFuture(
                        ToolExecutionResult.fail("[sandbox] " + blockedReason, "COMMAND_BLOCKED"));
            }

            String cwd = argMap.get("cwd") != null
                    ? String.valueOf(argMap.get("cwd"))
                    : defaultCwd;

            long timeoutMs = DEFAULT_TIMEOUT_MS;
            if (argMap.get("timeout_ms") != null) {
                try {
                    timeoutMs = ((Number) argMap.get("timeout_ms")).longValue();
                } catch (ClassCastException e) {
                    timeoutMs = Long.parseLong(String.valueOf(argMap.get("timeout_ms")));
                }
            }

            ShellResult result = executor.execute(command, cwd, timeoutMs);

            String output = formatResult(result);
            List<ContentBlock> content = List.of(new TextBlock(output));

            if (result.isSuccess()) {
                return CompletableFuture.completedFuture(ToolExecutionResult.ok(content));
            } else {
                return CompletableFuture.completedFuture(
                        ToolExecutionResult.fail(
                                "Command exited with code " + result.exitCode(),
                                "NON_ZERO_EXIT"));
            }
        } catch (Exception e) {
            return CompletableFuture.completedFuture(
                    ToolExecutionResult.fail("shell_execute failed: " + e.getMessage(), "EXECUTION_ERROR"));
        }
    }

    @Override
    public boolean isConcurrencySafe(Object args) { return false; }

    @Override
    public long timeoutMs() { return DEFAULT_TIMEOUT_MS + 10_000; }

    /**
     * 处理format Result相关逻辑。
     */
    private String formatResult(ShellResult r) {
        StringBuilder sb = new StringBuilder();
        sb.append("Exit code: ").append(r.exitCode());
        sb.append(" (").append(r.durationMs()).append("ms)\n");

        if (r.stdout() != null && !r.stdout().isEmpty()) {
            String stdout = r.stdout();
            if (stdout.length() > MAX_OUTPUT) {
                stdout = stdout.substring(0, MAX_OUTPUT) + "\n... [truncated, " + r.stdout().length() + " chars total]";
            }
            sb.append("--- stdout ---\n").append(stdout).append("\n");
        }
        if (r.stderr() != null && !r.stderr().isEmpty()) {
            String stderr = r.stderr();
            if (stderr.length() > MAX_OUTPUT) {
                stderr = stderr.substring(0, MAX_OUTPUT) + "\n... [truncated, " + r.stderr().length() + " chars total]";
            }
            sb.append("--- stderr ---\n").append(stderr).append("\n");
        }
        return sb.toString();
    }
}
