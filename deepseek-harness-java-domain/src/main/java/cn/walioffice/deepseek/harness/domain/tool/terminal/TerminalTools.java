package cn.walioffice.deepseek.harness.domain.tool.terminal;

import cn.walioffice.deepseek.harness.domain.llm.model.valobj.TextBlock;
import cn.walioffice.deepseek.harness.domain.terminal.adapter.port.ITerminalPort;
import cn.walioffice.deepseek.harness.domain.terminal.model.valobj.TerminalReadResultVO;
import cn.walioffice.deepseek.harness.domain.terminal.model.valobj.TerminalSendResultVO;
import cn.walioffice.deepseek.harness.domain.terminal.model.valobj.TerminalSessionSnapshotVO;
import cn.walioffice.deepseek.harness.domain.tool.model.entity.ToolDefinition;
import cn.walioffice.deepseek.harness.domain.tool.model.entity.ToolExecutionResult;
import cn.walioffice.deepseek.harness.domain.tool.model.entity.ToolRunContext;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 持久终端会话工具组（terminal_open/send/read/close/list）。
 * <p>
 * 与一次性 {@code shell_execute} 的差别：终端会话跨工具调用保持进程与
 * 环境变量（cd、export 等持久生效），适合交互式/多步命令。
 * Mirrors TS {@code tool-terminal}.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public final class TerminalTools {

    private TerminalTools() {}

    private abstract static class Base implements ToolDefinition {
        final ITerminalPort port;
        Base(ITerminalPort port) { this.port = port; }

        @SuppressWarnings("unchecked")
        static Map<String, Object> argMap(Object args) {
            return args instanceof Map ? (Map<String, Object>) args : Map.of();
        }

        static String str(Map<String, Object> m, String k) {
            Object v = m.get(k);
            return v == null ? "" : String.valueOf(v).trim();
        }

        static long longVal(Map<String, Object> m, String k, long def) {
            Object v = m.get(k);
            if (v instanceof Number n) return n.longValue();
            if (v == null) return def;
            try { return Long.parseLong(String.valueOf(v)); } catch (NumberFormatException e) { return def; }
        }

        static int intVal(Map<String, Object> m, String k, int def) {
            return (int) longVal(m, k, def);
        }

        CompletableFuture<ToolExecutionResult> ok(String text) {
            return CompletableFuture.completedFuture(
                    ToolExecutionResult.ok(List.of(new TextBlock(text))));
        }

        CompletableFuture<ToolExecutionResult> fail(String msg, String code) {
            return CompletableFuture.completedFuture(ToolExecutionResult.fail(msg, code));
        }

        static String snapshotLine(TerminalSessionSnapshotVO s) {
            return "- " + s.sessionId() + " [" + s.status() + "] " + s.name()
                    + " (cwd=" + s.cwd() + ", created " + s.createdAt() + ")";
        }
    }

    /** terminal_open：开启持久终端会话。 */
    public static final class Open extends Base {
        public Open(ITerminalPort port) { super(port); }
        @Override public String name() { return "terminal_open"; }
        @Override public String description() {
            return "Open a persistent terminal session (interactive shell). State such as cwd and env vars " +
                    "persists across terminal_send calls. Returns a session_id.";
        }
        @Override public Map<String, Object> parameters() {
            return Map.of("type", "object", "additionalProperties", false,
                    "properties", Map.of(
                            "name", Map.of("type", "string", "description", "Optional session name."),
                            "cwd", Map.of("type", "string", "description", "Optional working directory.")));
        }
        @Override public CompletableFuture<ToolExecutionResult> execute(Object args, ToolRunContext ctx) {
            Map<String, Object> m = argMap(args);
            try {
                TerminalSessionSnapshotVO s = port.open(str(m, "name").isEmpty() ? null : str(m, "name"),
                        str(m, "cwd").isEmpty() ? null : str(m, "cwd"));
                return ok("Opened terminal session:\n" + snapshotLine(s)
                        + "\nUse terminal_send(session_id=\"" + s.sessionId() + "\", command=...) to run commands.");
            } catch (Exception e) {
                return fail("terminal_open failed: " + e.getMessage(), "EXECUTION_ERROR");
            }
        }
    }

    /** terminal_send：向会话发送命令并等待输出。 */
    public static final class Send extends Base {
        public Send(ITerminalPort port) { super(port); }
        @Override public String name() { return "terminal_send"; }
        @Override public String description() {
            return "Send a command to a persistent terminal session and wait for its output. " +
                    "The session keeps its state (cwd, env) between sends.";
        }
        @Override public Map<String, Object> parameters() {
            return Map.of("type", "object", "additionalProperties", false,
                    "properties", Map.of(
                            "session_id", Map.of("type", "string", "description", "Terminal session id."),
                            "command", Map.of("type", "string", "description", "Command to send."),
                            "timeout_ms", Map.of("type", "integer", "description", "Optional wait timeout (default 120s).")),
                    "required", List.of("session_id", "command"));
        }
        @Override public CompletableFuture<ToolExecutionResult> execute(Object args, ToolRunContext ctx) {
            Map<String, Object> m = argMap(args);
            String sid = str(m, "session_id");
            String cmd = str(m, "command");
            if (sid.isEmpty() || cmd.isEmpty()) {
                return fail("session_id and command are required", "MISSING_PARAM");
            }
            try {
                TerminalSendResultVO r = port.send(sid, cmd, longVal(m, "timeout_ms", 0));
                StringBuilder sb = new StringBuilder();
                sb.append("session ").append(r.sessionId()).append(" [").append(r.status()).append("]");
                if (!r.completed()) sb.append(" (timed out waiting for completion)");
                if (r.truncated()) sb.append(" (output truncated)");
                sb.append("\n").append(r.output() == null ? "" : r.output());
                return ok(sb.toString());
            } catch (Exception e) {
                return fail("terminal_send failed: " + e.getMessage(), "EXECUTION_ERROR");
            }
        }
    }

    /** terminal_read：读取会话尾部输出。 */
    public static final class Read extends Base {
        public Read(ITerminalPort port) { super(port); }
        @Override public String name() { return "terminal_read"; }
        @Override public String description() {
            return "Read the latest output tail of a terminal session without sending a command.";
        }
        @Override public Map<String, Object> parameters() {
            return Map.of("type", "object", "additionalProperties", false,
                    "properties", Map.of(
                            "session_id", Map.of("type", "string", "description", "Terminal session id."),
                            "max_chars", Map.of("type", "integer", "description", "Max chars to read (default 8000).")),
                    "required", List.of("session_id"));
        }
        @Override public CompletableFuture<ToolExecutionResult> execute(Object args, ToolRunContext ctx) {
            Map<String, Object> m = argMap(args);
            String sid = str(m, "session_id");
            if (sid.isEmpty()) return fail("session_id is required", "MISSING_PARAM");
            try {
                TerminalReadResultVO r = port.read(sid, intVal(m, "max_chars", 0));
                return ok("session " + r.sessionId() + " [" + r.status() + "]"
                        + (r.truncated() ? " (truncated)" : "") + "\n"
                        + (r.content() == null ? "" : r.content()));
            } catch (Exception e) {
                return fail("terminal_read failed: " + e.getMessage(), "EXECUTION_ERROR");
            }
        }
        @Override public boolean isConcurrencySafe(Object args) { return true; }
    }

    /** terminal_close：关闭会话。 */
    public static final class Close extends Base {
        public Close(ITerminalPort port) { super(port); }
        @Override public String name() { return "terminal_close"; }
        @Override public String description() {
            return "Close a terminal session and terminate its process.";
        }
        @Override public Map<String, Object> parameters() {
            return Map.of("type", "object", "additionalProperties", false,
                    "properties", Map.of(
                            "session_id", Map.of("type", "string", "description", "Terminal session id.")),
                    "required", List.of("session_id"));
        }
        @Override public CompletableFuture<ToolExecutionResult> execute(Object args, ToolRunContext ctx) {
            Map<String, Object> m = argMap(args);
            String sid = str(m, "session_id");
            if (sid.isEmpty()) return fail("session_id is required", "MISSING_PARAM");
            try {
                TerminalSessionSnapshotVO s = port.close(sid);
                return ok("Closed terminal session:\n" + snapshotLine(s));
            } catch (Exception e) {
                return fail("terminal_close failed: " + e.getMessage(), "EXECUTION_ERROR");
            }
        }
    }

    /** terminal_list：列出会话。 */
    public static final class ListSessions extends Base {
        public ListSessions(ITerminalPort port) { super(port); }
        @Override public String name() { return "terminal_list"; }
        @Override public String description() {
            return "List all live terminal sessions with their status.";
        }
        @Override public Map<String, Object> parameters() {
            return Map.of("type", "object", "additionalProperties", false, "properties", Map.of());
        }
        @Override public CompletableFuture<ToolExecutionResult> execute(Object args, ToolRunContext ctx) {
            List<TerminalSessionSnapshotVO> sessions = port.list();
            if (sessions.isEmpty()) return ok("No live terminal sessions.");
            StringBuilder sb = new StringBuilder("Terminal sessions:\n");
            for (TerminalSessionSnapshotVO s : sessions) sb.append(snapshotLine(s)).append("\n");
            return ok(sb.toString());
        }
        @Override public boolean isConcurrencySafe(Object args) { return true; }
    }
}
