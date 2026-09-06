package cn.xiaofuge.deepseek.harness.infrastructure.adapter.e2b;

import cn.xiaofuge.deepseek.harness.domain.e2b.adapter.port.IE2BFsPort;
import java.util.List;

/** Stub E2B filesystem port for local mode (no real E2B sandbox). */
public class StubE2BFsPort implements IE2BFsPort {
    @Override public String readFile(String sandboxId, String path) { return ""; }
    @Override public void writeFile(String sandboxId, String path, String content) {}
    @Override public List<String> listDir(String sandboxId, String dir) { return List.of(); }
    @Override public void delete(String sandboxId, String path) {}
}
