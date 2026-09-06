package cn.xiaofuge.deepseek.harness.infrastructure.adapter.terminal;

import cn.xiaofuge.deepseek.harness.domain.terminal.adapter.port.ITerminalPort;
import cn.xiaofuge.deepseek.harness.domain.terminal.model.valobj.TerminalReadResultVO;
import cn.xiaofuge.deepseek.harness.domain.terminal.model.valobj.TerminalSendResultVO;
import cn.xiaofuge.deepseek.harness.domain.terminal.model.valobj.TerminalSessionSnapshotVO;
import cn.xiaofuge.deepseek.harness.domain.terminal.model.valobj.TerminalSessionStatus;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 本地终端端口实现。
 * <p>
 * 执行流程：
 * 1. 启动本地 shell 进程；
 * 2. 通过 stdin 写入命令并收集输出；
 * 3. 用内存会话表维护状态与缓冲区。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class LocalTerminalPort implements ITerminalPort {

    private static final int DEFAULT_MAX_OUTPUT_CHARS = 60_000;
    private static final int DEFAULT_READ_CHARS = 8_000;
    private static final long DEFAULT_WAIT_TIMEOUT_MS = 120_000L;

    private final String defaultCwd;
    private final Map<String, TerminalSessionRuntime> sessions = new ConcurrentHashMap<>();

    public LocalTerminalPort(String defaultCwd) {
        this.defaultCwd = defaultCwd;
    }

    @Override
    public TerminalSessionSnapshotVO open(String name, String cwd) {
        String sessionId = UUID.randomUUID().toString();
        String effectiveCwd = normalizeCwd(cwd);
        TerminalSessionRuntime runtime = new TerminalSessionRuntime(sessionId, normalizeName(name), effectiveCwd);
        sessions.put(sessionId, runtime);
        runtime.start();
        return runtime.snapshot();
    }

    @Override
    public TerminalSendResultVO send(String sessionId, String command, long timeoutMs) {
        TerminalSessionRuntime runtime = requireSession(sessionId);
        return runtime.send(command, timeoutMs > 0 ? timeoutMs : DEFAULT_WAIT_TIMEOUT_MS);
    }

    @Override
    public TerminalReadResultVO read(String sessionId, int maxChars) {
        TerminalSessionRuntime runtime = requireSession(sessionId);
        return runtime.read(maxChars > 0 ? maxChars : DEFAULT_READ_CHARS);
    }

    @Override
    public TerminalSessionSnapshotVO close(String sessionId) {
        TerminalSessionRuntime runtime = requireSession(sessionId);
        try {
            runtime.close();
        } finally {
            sessions.remove(sessionId);
        }
        return runtime.snapshot();
    }

    @Override
    public List<TerminalSessionSnapshotVO> list() {
        return sessions.values().stream()
                .map(TerminalSessionRuntime::snapshot)
                .sorted(Comparator.comparing(TerminalSessionSnapshotVO::createdAt).reversed())
                .toList();
    }

    private TerminalSessionRuntime requireSession(String sessionId) {
        TerminalSessionRuntime runtime = sessions.get(sessionId);
        if (runtime == null) {
            throw new IllegalArgumentException("terminal session not found: " + sessionId);
        }
        return runtime;
    }

    private String normalizeName(String name) {
        String value = name == null ? "" : name.trim();
        return value.isEmpty() ? "terminal" : value;
    }

    private String normalizeCwd(String cwd) {
        String value = cwd == null ? "" : cwd.trim();
        return value.isEmpty() ? defaultCwd : value;
    }

    private static final class TerminalSessionRuntime {
        private final String sessionId;
        private final String name;
        private final String cwd;
        private final Instant createdAt = Instant.now();
        private final StringBuilder buffer = new StringBuilder();
        private final Object lock = new Object();
        private volatile Process process;
        private volatile BufferedWriter writer;
        private volatile TerminalSessionStatus status = TerminalSessionStatus.RUNNING;
        private volatile Instant updatedAt = createdAt;
        private volatile Integer exitCode;
        private volatile String lastMessage = "";

        private TerminalSessionRuntime(String sessionId, String name, String cwd) {
            this.sessionId = sessionId;
            this.name = name;
            this.cwd = cwd;
        }

        private void start() {
            try {
                List<String> command = isWindows()
                        ? List.of("cmd.exe", "/Q", "/K")
                        : List.of("/bin/sh", "-i");
                ProcessBuilder builder = new ProcessBuilder(command);
                if (cwd != null && !cwd.isBlank()) {
                    builder.directory(new File(cwd));
                }
                builder.redirectErrorStream(true);
                Process started = builder.start();
                this.process = started;
                this.writer = new BufferedWriter(new OutputStreamWriter(started.getOutputStream(), StandardCharsets.UTF_8));
                Thread reader = new Thread(() -> readLoop(started.getInputStream()), "terminal-session-reader-" + sessionId);
                reader.setDaemon(true);
                reader.start();
                started.onExit().thenRun(() -> {
                    if (status != TerminalSessionStatus.CLOSED) {
                        exitCode = safeExitCode(started);
                        status = TerminalSessionStatus.EXITED;
                        updatedAt = Instant.now();
                    }
                });
            } catch (IOException e) {
                status = TerminalSessionStatus.EXITED;
                exitCode = -1;
                append("[terminal start error] " + e.getMessage() + "\n");
            }
        }

        private TerminalSendResultVO send(String command, long timeoutMs) {
            if (status != TerminalSessionStatus.RUNNING || process == null || writer == null) {
                throw new IllegalStateException("terminal session is not running: " + sessionId);
            }
            String marker = "__DSH_DONE_" + UUID.randomUUID().toString().replace("-", "") + "__";
            long startedAt = System.currentTimeMillis();
            String before = readTail(DEFAULT_MAX_OUTPUT_CHARS);
            try {
                synchronized (writer) {
                    writer.write(buildCommand(command, marker));
                    writer.flush();
                }
            } catch (IOException e) {
                return new TerminalSendResultVO(sessionId, command, "terminal write failed: " + e.getMessage(), false, false, status, exitCode, 0L, Instant.now());
            }

            boolean completed = waitForMarker(marker, timeoutMs);
            String after = readTail(DEFAULT_MAX_OUTPUT_CHARS);
            String delta = cutAfterMarker(before, after, marker);
            boolean truncated = delta.length() > DEFAULT_MAX_OUTPUT_CHARS;
            if (truncated) {
                delta = delta.substring(delta.length() - DEFAULT_MAX_OUTPUT_CHARS);
            }
            long duration = System.currentTimeMillis() - startedAt;
            return new TerminalSendResultVO(sessionId, command, delta, completed, truncated, status, exitCode, duration, Instant.now());
        }

        private TerminalReadResultVO read(int maxChars) {
            String content = readTail(maxChars);
            boolean truncated = content.length() >= maxChars && currentLength() > maxChars;
            return new TerminalReadResultVO(sessionId, content, truncated, status, exitCode);
        }

        private TerminalSessionSnapshotVO snapshot() {
            return new TerminalSessionSnapshotVO(
                    sessionId,
                    name,
                    cwd,
                    status,
                    createdAt,
                    updatedAt,
                    exitCode,
                    lastMessage,
                    currentLength()
            );
        }

        private void close() {
            status = TerminalSessionStatus.CLOSED;
            updatedAt = Instant.now();
            Process current = process;
            if (current != null) {
                current.destroy();
                try {
                    if (!current.waitFor(2, TimeUnit.SECONDS)) {
                        current.destroyForcibly();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    current.destroyForcibly();
                }
            }
        }

        private void readLoop(InputStream inputStream) {
            try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                char[] chars = new char[1024];
                int read;
                while ((read = reader.read(chars)) >= 0) {
                    if (read > 0) {
                        append(new String(chars, 0, read));
                    }
                    if (status == TerminalSessionStatus.CLOSED) {
                        break;
                    }
                }
            } catch (IOException e) {
                append("\n[terminal read error] " + e.getMessage() + "\n");
            }
        }

        private boolean waitForMarker(String marker, long timeoutMs) {
            long deadline = System.currentTimeMillis() + timeoutMs;
            while (System.currentTimeMillis() < deadline) {
                if (readTail(DEFAULT_MAX_OUTPUT_CHARS).contains(marker)) {
                    return true;
                }
                if (status != TerminalSessionStatus.RUNNING) {
                    return true;
                }
                try {
                    Thread.sleep(50L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return false;
        }

        private String buildCommand(String command, String marker) {
            if (isWindows()) {
                return command + " & echo " + marker + "\r\n";
            }
            return command + "; printf '\\n" + marker + "\\n'\n";
        }

        private String cutAfterMarker(String before, String after, String marker) {
            String delta = after.startsWith(before) ? after.substring(before.length()) : after;
            int markerIndex = delta.indexOf(marker);
            if (markerIndex >= 0) {
                delta = delta.substring(0, markerIndex);
            }
            return delta.trim();
        }

        private void append(String text) {
            synchronized (lock) {
                buffer.append(text);
                if (buffer.length() > DEFAULT_MAX_OUTPUT_CHARS) {
                    buffer.delete(0, buffer.length() - DEFAULT_MAX_OUTPUT_CHARS);
                }
                lastMessage = tail(Math.min(256, buffer.length()));
                updatedAt = Instant.now();
            }
        }

        private String readTail(int maxChars) {
            synchronized (lock) {
                return tail(maxChars);
            }
        }

        private int currentLength() {
            synchronized (lock) {
                return buffer.length();
            }
        }

        private String tail(int maxChars) {
            if (buffer.length() <= maxChars) {
                return buffer.toString();
            }
            return buffer.substring(buffer.length() - maxChars);
        }

        private int safeExitCode(Process started) {
            try {
                return started.exitValue();
            } catch (IllegalThreadStateException e) {
                return -1;
            }
        }

        private static boolean isWindows() {
            return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        }
    }
}
