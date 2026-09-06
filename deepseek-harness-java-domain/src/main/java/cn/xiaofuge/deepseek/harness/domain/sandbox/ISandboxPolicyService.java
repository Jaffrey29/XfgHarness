package cn.xiaofuge.deepseek.harness.domain.sandbox;

import cn.xiaofuge.deepseek.harness.domain.sandbox.model.valobj.SandboxExecutionPolicy;
import cn.xiaofuge.deepseek.harness.domain.sandbox.model.valobj.SandboxPolicyRequest;

/**
 * Application-facing sandbox-policy seam. The single owner of the deployment's
 * sandbox fallbacks plus per-session resolution.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface ISandboxPolicyService {

    /**
     * Resolve the execution policy for one capability call.
     *
     * @param request session cwd and optional mode override
     * @return resolved execution policy
     */
    SandboxExecutionPolicy resolve(SandboxPolicyRequest request);
}
