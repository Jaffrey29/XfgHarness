package cn.xiaofuge.deepseek.harness.domain.sandbox;

import cn.xiaofuge.deepseek.harness.domain.sandbox.model.valobj.SandboxExecutionPolicy;
import cn.xiaofuge.deepseek.harness.domain.sandbox.model.valobj.SandboxPolicyRequest;

/**
 * Application-facing sandbox-policy seam. The single owner of the deployment's
 * sandbox fallbacks plus per-session resolution.
 *
 * <p>调用流程：应用领域规则 → 更新聚合或值对象 → 发布事件/返回结果。</p>
 * <p>示例：领域对象只暴露业务语言方法，不直接依赖基础设施。</p>
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
