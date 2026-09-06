package cn.xiaofuge.deepseek.harness.infrastructure.adapter.coderuntime;

import cn.xiaofuge.deepseek.harness.domain.coderuntime.adapter.port.ICodeRuntimePort;
import cn.xiaofuge.deepseek.harness.domain.coderuntime.model.valobj.CodeRunRequest;
import cn.xiaofuge.deepseek.harness.domain.coderuntime.model.valobj.CodeRunResult;
import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

/**
 * Local code runtime: writes code to a temp file and executes via system interpreter.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class LocalCodeRuntimePort implements ICodeRuntimePort {

    private static final int DEFAULT_TIMEOUT_MS = 30_000;

    @Override
    public CodeRunResult execute(CodeRunRequest request) {
        String lang = request.language();
        if (lang == null) return CodeRunResult.fail("language is required", null);

        File script;
        try {
            script = Files.createTempFile("dsh-code-" + System.nanoTime(), "." + (lang.equals("python") ? "py" : "js")).toFile();
            Files.writeString(script.toPath(), request.code());
        } catch (Exception e) {
            return CodeRunResult.fail("cannot write temp file: " + e.getMessage(), null);
        }

        try {
            String cmd = lang.equals("python") ? "python3" : "node";
            ProcessBuilder pb = new ProcessBuilder(cmd, script.getAbsolutePath());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            byte[] output = process.getInputStream().readAllBytes();
            boolean finished = process.waitFor(DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return CodeRunResult.fail("code execution timed out after " + DEFAULT_TIMEOUT_MS + "ms", null);
            }

            String result = new String(output);
            int exitCode = process.exitValue();
            return exitCode == 0 ? CodeRunResult.ok(result.trim()) : CodeRunResult.fail(result.trim(), exitCode);
        } catch (Exception e) {
            return CodeRunResult.fail("execution failed: " + e.getMessage(), null);
        } finally {
            script.delete();
        }
    }
}
