package cn.walioffice.deepseek.harness.infrastructure.adapter.hooks;

import cn.walioffice.deepseek.harness.domain.hooks.adapter.port.IHookRunnerPort;
import cn.walioffice.deepseek.harness.domain.hooks.model.valobj.CommandHook;
import cn.walioffice.deepseek.harness.domain.hooks.model.valobj.HookOutput;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Runs hook commands via the local shell and parses their output.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class LocalShellHookRunner implements IHookRunnerPort {

    private static final int DEFAULT_TIMEOUT_MS = 60_000;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public HookOutput run(CommandHook hook, String payloadJson, Map<String, String> environment) {
        int timeoutMs = hook.timeoutSec() != null ? hook.timeoutSec() * 1000 : DEFAULT_TIMEOUT_MS;
        ProcessBuilder pb = new ProcessBuilder("sh", "-c", hook.command());
        pb.environment().putAll(environment);
        pb.redirectErrorStream(false);

        try {
            Process process = pb.start();
            if (payloadJson != null) {
                process.getOutputStream().write(payloadJson.getBytes(StandardCharsets.UTF_8));
                process.getOutputStream().close();
            }

            ByteArrayOutputStream stdoutBuf = new ByteArrayOutputStream();
            ByteArrayOutputStream stderrBuf = new ByteArrayOutputStream();
            process.getOutputStream().close();

            byte[] stdoutBytes = process.getInputStream().readAllBytes();
            byte[] stderrBytes = process.getErrorStream().readAllBytes();

            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new HookOutput(null, "hook timed out after " + timeoutMs + "ms", "",
                        Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty());
            }

            int exitCode = process.exitValue();
            String stdout = new String(stdoutBytes, StandardCharsets.UTF_8).trim();
            String stderr = new String(stderrBytes, StandardCharsets.UTF_8).trim();

            return parseOutput(exitCode, stdout, stderr);
        } catch (Exception e) {
            return new HookOutput(null, "hook execution failed: " + e.getMessage(), "",
                    Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty());
        }
    }

    private HookOutput parseOutput(int exitCode, String stdout, String stderr) {
        Optional<Boolean> continueFlag = Optional.empty();
        Optional<String> stopReason = Optional.empty();
        Optional<HookOutput.HookDecision> decision = Optional.empty();
        Optional<String> reason = Optional.empty();
        Optional<String> additionalContext = Optional.empty();
        Optional<String> systemMessage = Optional.empty();

        // Try to parse stdout as JSON
        if (!stdout.isEmpty() && stdout.trim().startsWith("{")) {
            try {
                var node = objectMapper.readTree(stdout);
                if (node.has("continue")) {
                    continueFlag = Optional.of(node.get("continue").asBoolean());
                }
                if (node.has("stopReason")) {
                    stopReason = Optional.of(node.get("stopReason").asText());
                }
                if (node.has("decision")) {
                    String d = node.get("decision").asText();
                    decision = parseDecision(d);
                }
                if (node.has("reason")) {
                    reason = Optional.of(node.get("reason").asText());
                }
                if (node.has("additionalContext")) {
                    additionalContext = Optional.of(node.get("additionalContext").asText());
                }
                if (node.has("systemMessage")) {
                    systemMessage = Optional.of(node.get("systemMessage").asText());
                }
                var hookSpecific = node.get("hookSpecificOutput");
                if (hookSpecific != null && hookSpecific.has("permissionDecision")) {
                    String pd = hookSpecific.get("permissionDecision").asText();
                    decision = parseDecision(pd);
                }
            } catch (Exception ignored) {
                // Non-JSON stdout: treat as additionalContext
                additionalContext = Optional.of(stdout);
            }
        }

        // Exit code 2 = block in CC
        if (exitCode == 2 && decision.isEmpty()) {
            decision = Optional.of(HookOutput.HookDecision.BLOCK);
            reason = Optional.of(stderr.isEmpty() ? "hook blocked (exit 2)" : stderr);
        }

        return new HookOutput(exitCode, stderr, stdout, continueFlag, stopReason,
                decision, reason, additionalContext, systemMessage);
    }

    private static Optional<HookOutput.HookDecision> parseDecision(String d) {
        if (d == null) return Optional.empty();
        return switch (d.toLowerCase()) {
            case "approve" -> Optional.of(HookOutput.HookDecision.APPROVE);
            case "allow" -> Optional.of(HookOutput.HookDecision.ALLOW);
            case "block" -> Optional.of(HookOutput.HookDecision.BLOCK);
            case "deny" -> Optional.of(HookOutput.HookDecision.DENY);
            case "ask" -> Optional.of(HookOutput.HookDecision.ASK);
            default -> Optional.empty();
        };
    }
}
