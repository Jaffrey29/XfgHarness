package cn.xiaofuge.deepseek.harness.domain.agent.service.run.tool;

import cn.xiaofuge.deepseek.harness.domain.agent.model.entity.AgentOptions;
import cn.xiaofuge.deepseek.harness.domain.agent.model.valobj.ApprovalModeVO;
import cn.xiaofuge.deepseek.harness.domain.tool.fs.adapter.port.IFsPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApprovalAwareFsPortTest {

    @TempDir
    Path tempDir;

    @Test
    void fullOpenWritesOutsideSandbox(@TempDir Path unrestrictedRoot) throws Exception {
        IFsPort sandbox = new ThrowingFsPort();
        IFsPort unrestricted = new RecordingFsPort(unrestrictedRoot);
        ApprovalAwareFsPort port = new ApprovalAwareFsPort(
                sandbox, unrestricted, () -> options(ApprovalModeVO.FULL_OPEN));

        Path target = unrestrictedRoot.resolve("allowed-by-full-open.txt");
        port.write(target.toString(), "ok");

        assertEquals("ok", Files.readString(target));
    }

    @Test
    void autoApproveRemainsSandboxed() {
        ApprovalAwareFsPort port = new ApprovalAwareFsPort(
                new ThrowingFsPort(), new RecordingFsPort(tempDir),
                () -> options(ApprovalModeVO.AUTO_APPROVE));

        assertThrows(SecurityException.class, () -> port.write("/tmp/denied.txt", "ok"));
    }

    private AgentOptions options(ApprovalModeVO approvalMode) {
        return new AgentOptions("channel", "provider", "model", 1024,
                null, "http://localhost", "key", "openai", approvalMode);
    }

    private static final class ThrowingFsPort implements IFsPort {
        @Override public String read(String path, int offset, int limit) { return ""; }
        @Override public void write(String path, String content) {
            throw new SecurityException("[sandbox: file access denied under workspace-write mode] " + path);
        }
        @Override public java.util.List<String> search(String directory, String pattern, int maxResults) { return java.util.List.of(); }
        @Override public boolean exists(String path) { return false; }
        @Override public java.util.List<DirEntry> listDir(String directory) { return java.util.List.of(); }
        @Override public java.util.List<String> grep(String directory, String pattern, String include, boolean isRegex, int maxResults) { return java.util.List.of(); }
        @Override public byte[] readBytes(String path) { return new byte[0]; }
    }

    private static final class RecordingFsPort implements IFsPort {
        private final Path root;
        private RecordingFsPort(Path root) { this.root = root; }
        @Override public String read(String path, int offset, int limit) { return ""; }
        @Override public void write(String path, String content) {
            try {
                Path resolved = Path.of(path).isAbsolute() ? Path.of(path) : root.resolve(path);
                Files.createDirectories(resolved.getParent());
                Files.writeString(resolved, content);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        @Override public java.util.List<String> search(String directory, String pattern, int maxResults) { return java.util.List.of(); }
        @Override public boolean exists(String path) { return false; }
        @Override public java.util.List<DirEntry> listDir(String directory) { return java.util.List.of(); }
        @Override public java.util.List<String> grep(String directory, String pattern, String include, boolean isRegex, int maxResults) { return java.util.List.of(); }
        @Override public byte[] readBytes(String path) { return new byte[0]; }
    }
}
