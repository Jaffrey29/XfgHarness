package cn.xiaofuge.deepseek.harness.domain.agent.service.run.tool;

import cn.xiaofuge.deepseek.harness.domain.agent.model.entity.AgentOptions;
import cn.xiaofuge.deepseek.harness.domain.agent.model.valobj.ApprovalModeVO;
import cn.xiaofuge.deepseek.harness.domain.tool.shell.IShellExecutor;
import cn.xiaofuge.deepseek.harness.domain.tool.shell.ShellResult;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApprovalAwareShellExecutorTest {

    @Test
    void fullOpenUsesUnrestrictedExecutor() {
        AtomicInteger calls = new AtomicInteger();
        IShellExecutor sandbox = (command, cwd, timeoutMs) -> new ShellResult("", "sandbox", -102, 0);
        IShellExecutor unrestricted = (command, cwd, timeoutMs) -> {
            calls.incrementAndGet();
            return new ShellResult("ok", "", 0, 0);
        };
        ApprovalAwareShellExecutor executor = new ApprovalAwareShellExecutor(
                sandbox, unrestricted, () -> options(ApprovalModeVO.FULL_OPEN));

        assertEquals(0, executor.execute("cd ~", "/tmp", 1000).exitCode());
        assertEquals(1, calls.get());
    }

    @Test
    void autoApproveRemainsSandboxed() {
        IShellExecutor sandbox = (command, cwd, timeoutMs) -> new ShellResult("", "sandbox", -102, 0);
        IShellExecutor unrestricted = (command, cwd, timeoutMs) -> new ShellResult("ok", "", 0, 0);
        ApprovalAwareShellExecutor executor = new ApprovalAwareShellExecutor(
                sandbox, unrestricted, () -> options(ApprovalModeVO.AUTO_APPROVE));

        assertEquals(-102, executor.execute("cd ~", "/tmp", 1000).exitCode());
    }

    private AgentOptions options(ApprovalModeVO approvalMode) {
        return new AgentOptions("channel", "provider", "model", 1024,
                null, "http://localhost", "key", "openai", approvalMode);
    }
}
