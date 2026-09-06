package cn.xiaofuge.deepseek.harness.domain.e2b;

import cn.xiaofuge.deepseek.harness.domain.e2b.adapter.port.IE2BFsPort;
import cn.xiaofuge.deepseek.harness.domain.e2b.adapter.port.IE2BSubprocessPort;
import cn.xiaofuge.deepseek.harness.domain.e2b.model.valobj.E2BSandboxEnv;
import java.util.Optional;

/**
 * Application-facing E2B sandbox seam: create, manage, and access sandboxes.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IE2BSandboxService {

    /**
     * Create a new sandbox.
     *
     * @param env sandbox environment configuration
     * @return sandbox id
     */
    String create(E2BSandboxEnv env);

    /**
     * Get the filesystem port for a sandbox.
     *
     * @param sandboxId sandbox identifier
     * @return filesystem port, or empty if sandbox not found
     */
    Optional<IE2BFsPort> fs(String sandboxId);

    /**
     * Get the subprocess port for a sandbox.
     *
     * @param sandboxId sandbox identifier
     * @return subprocess port, or empty if sandbox not found
     */
    Optional<IE2BSubprocessPort> subprocess(String sandboxId);

    /**
     * Kill and dispose a sandbox.
     *
     * @param sandboxId sandbox identifier
     */
    void kill(String sandboxId);
}
