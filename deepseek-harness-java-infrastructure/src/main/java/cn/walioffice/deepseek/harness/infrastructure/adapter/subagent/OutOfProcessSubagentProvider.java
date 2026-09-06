package cn.walioffice.deepseek.harness.infrastructure.adapter.subagent;

import cn.walioffice.deepseek.harness.domain.agent.subagent.SubagentProvider;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.ContentBlock;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.TextBlock;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/**
 * Base for out-of-process subagent providers: spawns a child process, sends the
 * prompt via stdin, and reads the assistant output from stdout.
 *
 * <p>Subclasses define the command, arguments, and wire protocol encoding.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public abstract class OutOfProcessSubagentProvider implements SubagentProvider {

    protected final String command;
    protected final List<String> args;
    protected final String cwd;
    protected final Map<String, String> env;
    protected final long timeoutMs;

    protected OutOfProcessSubagentProvider(String command, List<String> args, String cwd,
                                           Map<String, String> env, long timeoutMs) {
        this.command = command;
        this.args = List.copyOf(args);
        this.cwd = cwd;
        this.env = env == null ? Map.of() : Map.copyOf(env);
        this.timeoutMs = timeoutMs;
    }

    @Override
    public CompletableFuture<SubagentResult> start(SubagentStartRequest request) {
        return CompletableFuture.supplyAsync(() -> runChild(request));
    }

    @Override
    public SubagentCapabilities capabilities() {
        return new SubagentCapabilities(false, false, false);
    }

    private SubagentResult runChild(SubagentStartRequest request) {
        List<String> cmd = new ArrayList<>();
        cmd.add(command);
        cmd.addAll(args);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        if (cwd != null) pb.directory(new File(cwd));
        pb.environment().putAll(env);
        pb.redirectErrorStream(false);

        try {
            Process process = pb.start();
            String wireInput = encodePrompt(request);
            process.getOutputStream().write(wireInput.getBytes(StandardCharsets.UTF_8));
            process.getOutputStream().close();

            byte[] stdout = process.getInputStream().readAllBytes();
            byte[] stderr = process.getErrorStream().readAllBytes();

            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new SubagentResult("error",
                        List.of(new TextBlock("subagent timed out after " + timeoutMs + "ms")),
                        "timeout");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                String errText = new String(stderr, StandardCharsets.UTF_8).trim();
                return new SubagentResult("error",
                        List.of(new TextBlock("subagent exited with code " + exitCode + ": " + errText)),
                        "exit-" + exitCode);
            }

            String output = new String(stdout, StandardCharsets.UTF_8).trim();
            List<ContentBlock> blocks = decodeOutput(output);
            return new SubagentResult("completed", blocks, "ok");
        } catch (Exception e) {
            return new SubagentResult("error",
                    List.of(new TextBlock("subagent execution failed: " + e.getMessage())),
                    "exception");
        }
    }

    /** Encode the prompt into the wire format expected by the child process. */
    protected abstract String encodePrompt(SubagentStartRequest request);

    /** Decode the child's stdout into content blocks. */
    protected List<ContentBlock> decodeOutput(String output) {
        return List.of(new TextBlock(output));
    }
}
