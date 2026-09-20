package cn.xiaofuge.deepseek.harness.domain.agent.service.run.tool;

import cn.xiaofuge.deepseek.harness.domain.agent.model.entity.AgentOptions;
import cn.xiaofuge.deepseek.harness.domain.agent.model.valobj.ApprovalModeVO;
import cn.xiaofuge.deepseek.harness.domain.tool.shell.IShellExecutor;
import cn.xiaofuge.deepseek.harness.domain.tool.shell.ShellResult;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 审批模式感知的 Shell 执行器。
 * <p>仅在用户显式选择不受限模式时使用不受限执行器；其余模式继续走沙箱。</p>
 */
public final class ApprovalAwareShellExecutor implements IShellExecutor {

    private final IShellExecutor sandboxedExecutor;
    private final IShellExecutor unrestrictedExecutor;
    private final Supplier<AgentOptions> optionsSupplier;

    public ApprovalAwareShellExecutor(
            IShellExecutor sandboxedExecutor,
            IShellExecutor unrestrictedExecutor,
            Supplier<AgentOptions> optionsSupplier
    ) {
        this.sandboxedExecutor = Objects.requireNonNull(sandboxedExecutor);
        this.unrestrictedExecutor = Objects.requireNonNull(unrestrictedExecutor);
        this.optionsSupplier = Objects.requireNonNull(optionsSupplier);
    }

    @Override
    public ShellResult execute(String command, String cwd, long timeoutMs) {
        if (isFullOpen()) {
            return unrestrictedExecutor.execute(command, cwd, timeoutMs);
        }
        return sandboxedExecutor.execute(command, cwd, timeoutMs);
    }

    private boolean isFullOpen() {
        AgentOptions options = optionsSupplier.get();
        return options != null && options.approvalMode() == ApprovalModeVO.FULL_OPEN;
    }
}
