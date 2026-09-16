package cn.xiaofuge.deepseek.harness.domain.agent.service.run.tool;

import cn.xiaofuge.deepseek.harness.domain.agent.model.entity.AgentOptions;
import cn.xiaofuge.deepseek.harness.domain.agent.model.valobj.ApprovalModeVO;
import cn.xiaofuge.deepseek.harness.domain.tool.fs.adapter.port.IFsPort;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 审批模式感知的文件系统端口。
 * <p>读取操作始终走沙箱端口；写入操作在不受限模式下走不受限端口。</p>
 */
public final class ApprovalAwareFsPort implements IFsPort {

    private final IFsPort sandboxedPort;
    private final IFsPort unrestrictedPort;
    private final Supplier<AgentOptions> optionsSupplier;

    public ApprovalAwareFsPort(
            IFsPort sandboxedPort,
            IFsPort unrestrictedPort,
            Supplier<AgentOptions> optionsSupplier
    ) {
        this.sandboxedPort = Objects.requireNonNull(sandboxedPort);
        this.unrestrictedPort = Objects.requireNonNull(unrestrictedPort);
        this.optionsSupplier = Objects.requireNonNull(optionsSupplier);
    }

    @Override
    public String read(String path, int offset, int limit) {
        return sandboxedPort.read(path, offset, limit);
    }

    @Override
    public void write(String path, String content) {
        if (isFullOpen()) {
            unrestrictedPort.write(path, content);
            return;
        }
        sandboxedPort.write(path, content);
    }

    @Override
    public List<String> search(String directory, String pattern, int maxResults) {
        return sandboxedPort.search(directory, pattern, maxResults);
    }

    @Override
    public boolean exists(String path) {
        return sandboxedPort.exists(path);
    }

    @Override
    public List<DirEntry> listDir(String directory) {
        return sandboxedPort.listDir(directory);
    }

    @Override
    public List<String> grep(String directory, String pattern, String include, boolean isRegex, int maxResults) {
        return sandboxedPort.grep(directory, pattern, include, isRegex, maxResults);
    }

    @Override
    public byte[] readBytes(String path) {
        return sandboxedPort.readBytes(path);
    }

    private boolean isFullOpen() {
        AgentOptions options = optionsSupplier.get();
        return options != null && options.approvalMode() == ApprovalModeVO.FULL_OPEN;
    }
}
