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
     * 处理resolve。
     * <p>流程：校验入参 → 执行领域动作 → 返回处理结果。</p>
     * @param request 请求。
     * @return 领域处理结果。
     */
    SandboxExecutionPolicy resolve(SandboxPolicyRequest request);
}
